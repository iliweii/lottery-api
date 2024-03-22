package top.lucki.lottery.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.NumberUtils;
import top.lucki.lottery.common.constant.CommonConstant;
import top.lucki.lottery.common.constant.PermissionColumnType;
import top.lucki.lottery.common.constant.QueryRuleEnum;
import top.lucki.lottery.common.exception.BaseException;
import top.lucki.lottery.common.model.PermissionModel;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class QueryGenerator {
    public static final String SQL_RULES_COLUMN = "SQL_RULES_COLUMN";

    private static final String BEGIN = "_begin";
    private static final String END = "_end";
    /**
     * 数字类型字段，拼接此后缀 接受多值参数
     */
    private static final String MULTI = "_MultiString";
    private static final String STAR = "*";
    private static final String COMMA = ",";
    /**
     * 查询 逗号转义符 相当于一个逗号【作废】
     */
    public static final String QUERY_COMMA_ESCAPE = "++";
    private static final String NOT_EQUAL = "!";
    /**
     * 页面带有规则值查询，空格作为分隔符
     */
    private static final String QUERY_SEPARATE_KEYWORD = " ";
    /**
     * 单引号
     */
    public static final String SQL_SQ = "'";
    /**
     * 排序列
     */
    private static final String ORDER_COLUMN = "column";
    /**
     * 排序方式
     */
    private static final String ORDER_TYPE = "order";
    private static final String ORDER_TYPE_ASC = "ASC";


    /**
     * 时间格式化
     */
    private static final ThreadLocal<SimpleDateFormat> local = new ThreadLocal<SimpleDateFormat>();

    private static SimpleDateFormat getTime() {
        SimpleDateFormat time = local.get();
        if (time == null) {
            time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            local.set(time);
        }
        return time;
    }

    /**
     * 获取查询条件构造器QueryWrapper实例 通用查询条件已被封装完成
     *
     * @param searchObj    查询实体
     * @param parameterMap request.getParameterMap()
     * @return QueryWrapper实例
     */
    public static <T> QueryWrapper<T> initQueryWrapper(T searchObj, Map<String, String[]> parameterMap) {
        long start = System.currentTimeMillis();
        QueryWrapper<T> queryWrapper = new QueryWrapper<T>();
        installMplus(queryWrapper, searchObj, parameterMap);
        log.debug("---查询条件构造器初始化完成,耗时:" + (System.currentTimeMillis() - start) + "毫秒----");
        return queryWrapper;
    }

    /**
     * 组装Mybatis Plus 查询条件
     * <p>使用此方法 需要有如下几点注意:
     * <br>1.使用QueryWrapper 而非LambdaQueryWrapper;
     * <br>2.实例化QueryWrapper时不可将实体传入参数
     * <br>错误示例:如QueryWrapper<JeecgDemo> queryWrapper = new QueryWrapper<JeecgDemo>(jeecgDemo);
     * <br>正确示例:QueryWrapper<JeecgDemo> queryWrapper = new QueryWrapper<JeecgDemo>();
     * <br>3.也可以不使用这个方法直接调用 {@link #initQueryWrapper}直接获取实例
     */
    public static void installMplus(QueryWrapper<?> queryWrapper, Object searchObj, Map<String, String[]> parameterMap) {

		/*
		 * 注意:权限查询由前端配置数据规则 当一个人有多个所属部门时候 可以在规则配置包含条件 orgCode 包含 #{sys_org_code}
		但是不支持在自定义SQL中写orgCode in #{sys_org_code}
		当一个人只有一个部门 就直接配置等于条件: orgCode 等于 #{sys_org_code} 或者配置自定义SQL: orgCode = '#{sys_org_code}'
		*/

        //如果通过查询构造器查询查询id条件，则取消其他查询条件（包括数据权限），主要用于历史数据，或者失效数据的回显
        if (parameterMap.get("id") != null && parameterMap.get("id").length == 1) {
            String ids[] = parameterMap.get("id")[0].split(",");
            if (ids.length <= 1) {
                queryWrapper.eq("id", parameterMap.get("id")[0]);
            } else {
                queryWrapper.in("id", ids);
            }
            return;
        }

        //区间条件组装 模糊查询 高级查询组装 简单排序 权限查询
        PropertyDescriptor origDescriptors[] = PropertyUtils.getPropertyDescriptors(searchObj);
        Map<String, PermissionModel> ruleMap = getRuleMap();

        //权限规则自定义SQL表达式
        //数据权限在这里直接进行配置，不需要再判断模型中是否存在字段，字段类型需要维护，目前只有4种枚举
        for (String c : ruleMap.keySet()) {
            if (StrUtil.isNotEmpty(c) && c.startsWith(SQL_RULES_COLUMN)) {
                queryWrapper.and(i -> i.apply(getSqlRuleValue(ruleMap.get(c).getRuleValue())));
            } else if (ObjectUtil.isNotNull(ruleMap.get(c))) {
                PermissionModel permissionModel = ruleMap.get(c);
                addRuleToQueryWrapper(permissionModel, permissionModel.getRuleColumn(), PermissionColumnType.getEnum(permissionModel.getColumnType()).getValue(), queryWrapper);
            }
        }

        String name, type, column;
        //定义实体字段和数据库字段名称的映射 高级查询中 只能获取实体字段 如果设置TableField注解 那么查询条件会出问题
        Map<String, String> fieldColumnMap = new HashMap<String, String>();
        for (int i = 0; i < origDescriptors.length; i++) {
            //aliasName = origDescriptors[i].getName();  mybatis  不存在实体属性 不用处理别名的情况
            name = origDescriptors[i].getName();
            type = origDescriptors[i].getPropertyType().toString();
            try {
                if (judgedIsUselessField(name) || !PropertyUtils.isReadable(searchObj, name)) {
                    continue;
                }

                Object value = PropertyUtils.getSimpleProperty(searchObj, name);
                column = getTableFieldName(searchObj.getClass(), name);
                if (column == null) {
                    //column为null只有一种情况 那就是 添加了注解@TableField(exist = false) 后续都不用处理了
                    continue;
                }
                fieldColumnMap.put(name, column);
                //数据权限查询
//				if(ruleMap.containsKey(name)) {
//					addRuleToQueryWrapper(ruleMap.get(name), column, origDescriptors[i].getPropertyType(), queryWrapper);
//				}
                //区间查询
                doIntervalQuery(queryWrapper, parameterMap, type, name, column);
                //判断单值  参数带不同标识字符串 走不同的查询
                //TODO 这种前后带逗号的支持分割后模糊查询需要否 使多选字段的查询生效
                if (null != value && value.toString().startsWith(COMMA) && value.toString().endsWith(COMMA)) {
                    String multiLikeval = value.toString().replace(",,", COMMA);
                    String[] vals = multiLikeval.substring(1, multiLikeval.length()).split(COMMA);
                    final String field = StrUtil.toUnderlineCase(column);
                    if (vals.length > 1) {
                        queryWrapper.and(j -> {
                            j = j.like(field, vals[0]);
                            for (int k = 1; k < vals.length; k++) {
                                j = j.or().like(field, vals[k]);
                            }
                            //return j;
                        });
                    } else {
                        queryWrapper.and(j -> j.like(field, vals[0]));
                    }
                } else {
                    //根据参数值带什么关键字符串判断走什么类型的查询
                    QueryRuleEnum rule = convert2Rule(value);
                    value = replaceValue(rule, value);
                    // add -begin 添加判断为字符串时设为全模糊查询
                    //if( (rule==null || QueryRuleEnum.EQ.equals(rule)) && "class java.lang.String".equals(type)) {
                    // 可以设置左右模糊或全模糊，因人而异
                    //rule = QueryRuleEnum.LIKE;
                    //}
                    // add -end 添加判断为字符串时设为全模糊查询
                    addEasyQuery(queryWrapper, column, rule, value);
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        // 排序逻辑 处理
        doMultiFieldsOrder(queryWrapper, parameterMap);

        //@TableLogic业务处理
        useTableLogic(queryWrapper, searchObj.getClass(), searchObj);
    }

    /**
     * 用于使用查询构造器时，多表查询@TableLogic失效
     * 但是查询的字段中必须要有@TableLogic修饰的字段
     *
     * @param queryWrapper
     * @param clazz
     */
    public static void useTableLogic(QueryWrapper<?> queryWrapper, Class<?> clazz, Object object) {
        List<Field> classFields = getClassFields(clazz);
        if (CollUtil.isNotEmpty(classFields)) {
            classFields.forEach(field -> {
                TableLogic tableLogic = field.getAnnotation(TableLogic.class);
                Object value = null;
                try {
                    field.setAccessible(true);
                    value = field.get(object);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new BaseException("查询异常");
                }
                if (tableLogic != null && ObjectUtil.isNull(value)) {
                    String column = field.getName();
                    addEasyQuery(queryWrapper, column, QueryRuleEnum.EQ, 0);
                }
            });
        }
    }


    /**
     * 区间查询
     *
     * @param queryWrapper query对象
     * @param parameterMap 参数map
     * @param type         字段类型
     * @param filedName    字段名称
     * @param columnName   列名称
     */
    private static void doIntervalQuery(QueryWrapper<?> queryWrapper, Map<String, String[]> parameterMap, String type, String filedName, String columnName) throws ParseException {
        // 添加 判断是否有区间值
        String endValue = null, beginValue = null;
        if (parameterMap != null && parameterMap.containsKey(filedName + BEGIN)) {
            beginValue = parameterMap.get(filedName + BEGIN)[0].trim();
            addQueryByRule(queryWrapper, columnName, type, beginValue, QueryRuleEnum.GE);

        }
        if (parameterMap != null && parameterMap.containsKey(filedName + END)) {
            endValue = parameterMap.get(filedName + END)[0].trim();
            addQueryByRule(queryWrapper, columnName, type, endValue, QueryRuleEnum.LE);
        }
        //多值查询
        if (parameterMap != null && parameterMap.containsKey(filedName + MULTI)) {
            endValue = parameterMap.get(filedName + MULTI)[0].trim();
            addQueryByRule(queryWrapper, columnName.replace(MULTI, ""), type, endValue, QueryRuleEnum.IN);
        }
    }

    //多字段排序 TODO 需要修改前端
    public static void doMultiFieldsOrder(QueryWrapper<?> queryWrapper, Map<String, String[]> parameterMap) {
        String column = null, order = null;
        if (parameterMap != null && parameterMap.containsKey(ORDER_COLUMN)) {
            column = parameterMap.get(ORDER_COLUMN)[0];
        }
        if (parameterMap != null && parameterMap.containsKey(ORDER_TYPE)) {
            order = parameterMap.get(ORDER_TYPE)[0];
        }
        log.debug("排序规则>>列:" + column + ",排序方式:" + order);
        if (StrUtil.isNotEmpty(column) && StrUtil.isNotEmpty(order)) {
            //字典字段，去掉字典翻译文本后缀
            if (column.endsWith(CommonConstant.DICT_TEXT_SUFFIX)) {
                column = column.substring(0, column.lastIndexOf(CommonConstant.DICT_TEXT_SUFFIX));
            }
            //SQL注入check
            SqlInjectionUtil.filterContent(column);

            if (order.toUpperCase().indexOf(ORDER_TYPE_ASC) >= 0) {
                queryWrapper.orderByAsc(StrUtil.toUnderlineCase(column));
            } else {
                queryWrapper.orderByDesc(StrUtil.toUnderlineCase(column));
            }
        }
    }

    /**
     * 根据所传的值 转化成对应的比较方式
     * 支持><= like in !
     *
     * @param value
     * @return
     */
    private static QueryRuleEnum convert2Rule(Object value) {
        // 避免空数据
        if (value == null) {
            return null;
        }
        String val = (value + "").toString().trim();
        if (val.length() == 0) {
            return null;
        }
        QueryRuleEnum rule = null;

        //update-begin--Author:scott  Date:20190724 for：initQueryWrapper组装sql查询条件错误 #284-------------------
        //TODO 此处规则，只适用于 le lt ge gt
        // step 2 .>= =<
        if (rule == null && val.length() >= 3) {
            if (QUERY_SEPARATE_KEYWORD.equals(val.substring(2, 3))) {
                rule = QueryRuleEnum.getByValue(val.substring(0, 2));
            }
        }
        // step 1 .> <
        if (rule == null && val.length() >= 2) {
            if (QUERY_SEPARATE_KEYWORD.equals(val.substring(1, 2))) {
                rule = QueryRuleEnum.getByValue(val.substring(0, 1));
            }
        }
        //update-end--Author:scott  Date:20190724 for：initQueryWrapper组装sql查询条件错误 #284---------------------

        // step 3 like
        if (rule == null && val.contains(STAR)) {
            if (val.startsWith(STAR) && val.endsWith(STAR)) {
                rule = QueryRuleEnum.LIKE;
            } else if (val.startsWith(STAR)) {
                rule = QueryRuleEnum.LEFT_LIKE;
            } else if (val.endsWith(STAR)) {
                rule = QueryRuleEnum.RIGHT_LIKE;
            }
        }

        // step 4 in
        if (rule == null && val.contains(COMMA)) {
            //TODO in 查询这里应该有个bug  如果一字段本身就是多选 此时用in查询 未必能查询出来
            rule = QueryRuleEnum.IN;
        }
        // step 5 !=
        if (rule == null && val.startsWith(NOT_EQUAL)) {
            rule = QueryRuleEnum.NE;
        }
        // step 6 xx+xx+xx 这种情况适用于如果想要用逗号作精确查询 但是系统默认逗号走in 所以可以用++替换【此逻辑作废】
        if (rule == null && val.indexOf(QUERY_COMMA_ESCAPE) > 0) {
            rule = QueryRuleEnum.EQ_WITH_ADD;
        }

        //update-begin--Author:taoyan  Date:20201229 for：initQueryWrapper组装sql查询条件错误 #284---------------------
        //特殊处理：Oracle的表达式to_date('xxx','yyyy-MM-dd')含有逗号，会被识别为in查询，转为等于查询
        if (rule == QueryRuleEnum.IN && val.indexOf("yyyy-MM-dd") >= 0 && val.indexOf("to_date") >= 0) {
            rule = QueryRuleEnum.EQ;
        }
        //update-end--Author:taoyan  Date:20201229 for：initQueryWrapper组装sql查询条件错误 #284---------------------

        return rule != null ? rule : QueryRuleEnum.EQ;
    }

    /**
     * 替换掉关键字字符
     *
     * @param rule
     * @param value
     * @return
     */
    private static Object replaceValue(QueryRuleEnum rule, Object value) {
        if (rule == null) {
            return null;
        }
        if (!(value instanceof String)) {
            return value;
        }
        String val = (value + "").toString().trim();
        if (rule == QueryRuleEnum.LIKE) {
            value = val.substring(1, val.length() - 1);
        } else if (rule == QueryRuleEnum.LEFT_LIKE || rule == QueryRuleEnum.NE) {
            value = val.substring(1);
        } else if (rule == QueryRuleEnum.RIGHT_LIKE) {
            value = val.substring(0, val.length() - 1);
        } else if (rule == QueryRuleEnum.IN) {
            value = val.split(",");
        } else if (rule == QueryRuleEnum.EQ_WITH_ADD) {
            value = val.replaceAll("\\+\\+", COMMA);
        } else {
            //update-begin--Author:scott  Date:20190724 for：initQueryWrapper组装sql查询条件错误 #284-------------------
            if (val.startsWith(rule.getValue())) {
                //TODO 此处逻辑应该注释掉-> 如果查询内容中带有查询匹配规则符号，就会被截取的（比如：>=您好）
                value = val.replaceFirst(rule.getValue(), "");
            } else if (val.startsWith(rule.getCondition() + QUERY_SEPARATE_KEYWORD)) {
                value = val.replaceFirst(rule.getCondition() + QUERY_SEPARATE_KEYWORD, "").trim();
            }
            //update-end--Author:scott  Date:20190724 for：initQueryWrapper组装sql查询条件错误 #284-------------------
        }
        return value;
    }

    private static void addQueryByRule(QueryWrapper<?> queryWrapper, String name, String type, String value, QueryRuleEnum rule) throws ParseException {
        if (StrUtil.isNotEmpty(value)) {
            Object temp;
            // 针对数字类型字段，多值查询
            if (value.indexOf(COMMA) != -1) {
                temp = value;
                addEasyQuery(queryWrapper, name, rule, temp);
                return;
            }

            switch (type) {
                case "class java.lang.Integer":
                    temp = Integer.parseInt(value);
                    break;
                case "class java.math.BigDecimal":
                    temp = new BigDecimal(value);
                    break;
                case "class java.lang.Short":
                    temp = Short.parseShort(value);
                    break;
                case "class java.lang.Long":
                    temp = Long.parseLong(value);
                    break;
                case "class java.lang.Float":
                    temp = Float.parseFloat(value);
                    break;
                case "class java.lang.Double":
                    temp = Double.parseDouble(value);
                    break;
                case "class java.util.Date":
                    temp = getDateQueryByRule(value, rule);
                    break;
                default:
                    temp = value;
                    break;
            }
            addEasyQuery(queryWrapper, name, rule, temp);
        }
    }

    /**
     * 获取日期类型的值
     *
     * @param value
     * @param rule
     * @return
     * @throws ParseException
     */
    private static Date getDateQueryByRule(String value, QueryRuleEnum rule) throws ParseException {
        Date date = null;
        if (value.length() == 10) {
            if (rule == QueryRuleEnum.GE) {
                //比较大于
                date = getTime().parse(value + " 00:00:00");
            } else if (rule == QueryRuleEnum.LE) {
                //比较小于
                date = getTime().parse(value + " 23:59:59");
            }
            //TODO 日期类型比较特殊 可能oracle下不一定好使
        }
        if (date == null) {
            date = getTime().parse(value);
        }
        return date;
    }

    /**
     * 根据规则走不同的查询
     *
     * @param queryWrapper QueryWrapper
     * @param name         字段名字
     * @param rule         查询规则
     * @param value        查询条件值
     */
    private static void addEasyQuery(QueryWrapper<?> queryWrapper, String name, QueryRuleEnum rule, Object value) {
        if (value == null || rule == null || ObjectUtil.isEmpty(value)) {
            return;
        }
        name = StrUtil.toUnderlineCase(name);
        log.info("--查询规则-->" + name + " " + rule.getValue() + " " + value);
        switch (rule) {
            case GT:
                queryWrapper.gt(name, value);
                break;
            case GE:
                queryWrapper.ge(name, value);
                break;
            case LT:
                queryWrapper.lt(name, value);
                break;
            case LE:
                queryWrapper.le(name, value);
                break;
            case EQ:
            case EQ_WITH_ADD:
                queryWrapper.eq(name, value);
                break;
            case NE:
                //需要将value中的"!"去掉
                value = value.toString().replace("!", "");
                queryWrapper.ne(name, value);
                break;
            case IN:
                if (value instanceof String) {
                    queryWrapper.in(name, (Object[]) value.toString().split(","));
                } else if (value instanceof String[]) {
                    queryWrapper.in(name, (Object[]) value);
                }
                //update-begin-author:taoyan date:20200909 for:【bug】in 类型多值查询 不适配postgresql #1671
                else if (value.getClass().isArray()) {
                    queryWrapper.in(name, (Object[]) value);
                } else {
                    queryWrapper.in(name, value);
                }
                //update-end-author:taoyan date:20200909 for:【bug】in 类型多值查询 不适配postgresql #1671
                break;
            case LIKE:
                queryWrapper.like(name, value);
                break;
            case LEFT_LIKE:
                queryWrapper.likeLeft(name, value);
                break;
            case RIGHT_LIKE:
                queryWrapper.likeRight(name, value);
                break;
            default:
                log.info("--查询规则未匹配到---");
                break;
        }
    }

    /**
     * @param name
     * @return
     */
    private static boolean judgedIsUselessField(String name) {
        return "class".equals(name) || "ids".equals(name)
                || "page".equals(name) || "rows".equals(name)
                || "sort".equals(name) || "order".equals(name);
    }


    /**
     * 获取请求对应的数据权限规则
     *
     * @return
     */
    public static Map<String, PermissionModel> getRuleMap() {
        Map<String, PermissionModel> ruleMap = new HashMap<String, PermissionModel>();
        List<PermissionModel> list = new LinkedList<>(); // TODO 查询规则
        if (list != null && list.size() > 0) {
            if (list.get(0) == null) {
                return ruleMap;
            }
            for (PermissionModel rule : list) {
                String column = rule.getRuleColumn();
                if (QueryRuleEnum.SQL_RULES.getValue().equals(rule.getRuleConditions())) {
                    column = SQL_RULES_COLUMN + rule.getId();
                }
                ruleMap.put(column, rule);
            }
        }
        return ruleMap;
    }

    private static void addRuleToQueryWrapper(PermissionModel dataRule, String name, Class propertyType, QueryWrapper<?> queryWrapper) {
        QueryRuleEnum rule = QueryRuleEnum.getByValue(dataRule.getRuleConditions());
        if (rule.equals(QueryRuleEnum.IN) && !propertyType.equals(String.class)) {
            String[] values = dataRule.getRuleValue().split(",");
            Object[] objs = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                objs[i] = NumberUtils.parseNumber(values[i], propertyType);
            }
            addEasyQuery(queryWrapper, name, rule, objs);
        } else {
            if (propertyType.equals(String.class)) {
                addEasyQuery(queryWrapper, name, rule, converRuleValue(dataRule.getRuleValue()));
            } else if (propertyType.equals(Date.class)) {
                String dateStr = converRuleValue(dataRule.getRuleValue());
                if (dateStr.length() == 10) {
                    addEasyQuery(queryWrapper, name, rule, DateUtil.parse(dateStr, "yyyy-MM-dd"));
                } else {
                    addEasyQuery(queryWrapper, name, rule, DateUtil.parse(dateStr, "yyyy-MM-dd HH:mm:ss"));
                }
            } else {
                addEasyQuery(queryWrapper, name, rule, NumberUtils.parseNumber(dataRule.getRuleValue(), propertyType));
            }
        }
    }

    public static String converRuleValue(String ruleValue) {
        String value;
//        value = JwtUtil.getUserSystemData(ruleValue, null);
        value = "";
        // 获取request里面对应的值，如果有可以动态拼接参数，如果没有可能就会导致查询异常！
        if (StrUtil.isEmpty(value) && ruleValue.contains("#{")) {
            String requestKey = ruleValue.substring(2, ruleValue.indexOf("}"));
            Map<String, String[]> parameterMap = SpringContextUtils.getHttpServletRequest().getParameterMap();
            String[] strings = parameterMap.get(requestKey);
            if (strings != null && strings.length > 0) {
                value = strings[0];
            }
        }
        return value != null ? value : ruleValue;
    }

    /**
     * 去掉值前后单引号
     */
    public static String trimSingleQuote(String ruleValue) {
        if (StrUtil.isEmpty(ruleValue)) {
            return "";
        }
        if (ruleValue.startsWith(QueryGenerator.SQL_SQ)) {
            ruleValue = ruleValue.substring(1);
        }
        if (ruleValue.endsWith(QueryGenerator.SQL_SQ)) {
            ruleValue = ruleValue.substring(0, ruleValue.length() - 1);
        }
        return ruleValue;
    }

    public static String getSqlRuleValue(String sqlRule) {
        try {
            Set<String> varParams = getSqlRuleParams(sqlRule);
            for (String var : varParams) {
                String tempValue = converRuleValue(var);
                sqlRule = sqlRule.replace("#{" + var + "}", tempValue);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return sqlRule;
    }

    /**
     * 获取sql中的#{key} 这个key组成的set
     */
    public static Set<String> getSqlRuleParams(String sql) {
        if (StrUtil.isEmpty(sql)) {
            return null;
        }
        Set<String> varParams = new HashSet<String>();
        String regex = "\\#\\{\\w+\\}";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String var = m.group();
            varParams.add(var.substring(var.indexOf("{") + 1, var.indexOf("}")));
        }
        return varParams;
    }

    /**
     * 获取查询条件
     *
     * @param field
     * @param alias
     * @param value
     * @param isString
     * @return
     */
    public static String getSingleQueryConditionSql(String field, String alias, Object value, boolean isString) {
        return getSingleQueryConditionSql(field, alias, value, isString, null);
    }

    /**
     * 报表获取查询条件 支持多数据源
     *
     * @param field
     * @param alias
     * @param value
     * @param isString
     * @param dataBaseType
     * @return
     */
    public static String getSingleQueryConditionSql(String field, String alias, Object value, boolean isString, String dataBaseType) {
        if (value == null) {
            return "";
        }
        field = alias + StrUtil.toUnderlineCase(field);
        QueryRuleEnum rule = QueryGenerator.convert2Rule(value);
        return getSingleSqlByRule(rule, field, value, isString, dataBaseType);
    }

    /**
     * 获取单个查询条件的值
     *
     * @param rule
     * @param field
     * @param value
     * @param isString
     * @param dataBaseType
     * @return
     */
    public static String getSingleSqlByRule(QueryRuleEnum rule, String field, Object value, boolean isString, String dataBaseType) {
        String res = "";
        switch (rule) {
            case GT:
                res = field + rule.getValue() + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case GE:
                res = field + rule.getValue() + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case LT:
                res = field + rule.getValue() + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case LE:
                res = field + rule.getValue() + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case EQ:
                res = field + rule.getValue() + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case EQ_WITH_ADD:
                res = field + " = " + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case NE:
                res = field + " <> " + getFieldConditionValue(value, isString, dataBaseType);
                break;
            case IN:
                res = field + " in " + getInConditionValue(value, isString);
                break;
            case LIKE:
                res = field + " like " + getLikeConditionValue(value);
                break;
            case LEFT_LIKE:
                res = field + " like " + getLikeConditionValue(value);
                break;
            case RIGHT_LIKE:
                res = field + " like " + getLikeConditionValue(value);
                break;
            default:
                res = field + " = " + getFieldConditionValue(value, isString, dataBaseType);
                break;
        }
        return res;
    }


    /**
     * 获取单个查询条件的值
     *
     * @param rule
     * @param field
     * @param value
     * @param isString
     * @return
     */
    public static String getSingleSqlByRule(QueryRuleEnum rule, String field, Object value, boolean isString) {
        return getSingleSqlByRule(rule, field, value, isString, null);
    }

    /**
     * 获取查询条件的值
     *
     * @param value
     * @param isString
     * @param dataBaseType
     * @return
     */
    private static String getFieldConditionValue(Object value, boolean isString, String dataBaseType) {
        String str = value.toString().trim();
        if (str.startsWith("!")) {
            str = str.substring(1);
        } else if (str.startsWith(">=")) {
            str = str.substring(2);
        } else if (str.startsWith("<=")) {
            str = str.substring(2);
        } else if (str.startsWith(">")) {
            str = str.substring(1);
        } else if (str.startsWith("<")) {
            str = str.substring(1);
        } else if (str.indexOf(QUERY_COMMA_ESCAPE) > 0) {
            str = str.replaceAll("\\+\\+", COMMA);
        }
        if (isString) {
            return " '" + str + "' ";
        } else {
            return value.toString();
        }
    }

    private static String getInConditionValue(Object value, boolean isString) {
        if (isString) {
            String temp[] = value.toString().split(",");
            String res = "";
            for (String string : temp) {
                res += ",N'" + string + "'";
            }
            return "(" + res.substring(1) + ")";
        } else {
            return "(" + value.toString() + ")";
        }
    }

    private static String getLikeConditionValue(Object value) {
        String str = value.toString().trim();
        if (str.startsWith("*") && str.endsWith("*")) {
            return "'%" + str.substring(1, str.length() - 1) + "%'";
        } else if (str.startsWith("*")) {
            return "N'%" + str.substring(1) + "'";
        } else if (str.endsWith("*")) {
            return "N'" + str.substring(0, str.length() - 1) + "%'";
        } else {
            if (str.indexOf("%") >= 0) {
                if (str.startsWith("'") && str.endsWith("'")) {
                    return "N" + str;
                } else {
                    return "N" + "'" + str + "'";
                }
            } else {
                return "N'%" + str + "%'";
            }
        }
    }

    /**
     * 转换sql中的系统变量
     *
     * @param sql
     * @return
     */
    public static String convertSystemVariables(String sql) {
        return getSqlRuleValue(sql);
    }

    /**
     * 获取所有配置的权限 返回sql字符串 不受字段限制 配置什么就拿到什么
     *
     * @return
     */
    public static String getAllConfigAuth() {
        StringBuffer sb = new StringBuffer();
        //权限查询
        Map<String, PermissionModel> ruleMap = getRuleMap();
        String sql_and = " and ";
        for (String c : ruleMap.keySet()) {
            PermissionModel dataRule = ruleMap.get(c);
            String ruleValue = dataRule.getRuleValue();
            if (StrUtil.isEmpty(ruleValue)) {
                continue;
            }
            if (StrUtil.isNotEmpty(c) && c.startsWith(SQL_RULES_COLUMN)) {
                sb.append(sql_and + getSqlRuleValue(ruleValue));
            } else {
                boolean isString = false;
                ruleValue = ruleValue.trim();
                if (ruleValue.startsWith("'") && ruleValue.endsWith("'")) {
                    isString = true;
                    ruleValue = ruleValue.substring(1, ruleValue.length() - 1);
                }
                QueryRuleEnum rule = QueryRuleEnum.getByValue(dataRule.getRuleConditions());
                String value = converRuleValue(ruleValue);
                String filedSql = getSingleSqlByRule(rule, c, value, isString);
                sb.append(sql_and + filedSql);
            }
        }
        log.info("query auth sql is = " + sb.toString());
        return sb.toString();
    }

    /**
     * 获取class的 包括父类的
     *
     * @param clazz
     * @return
     */
    private static List<Field> getClassFields(Class<?> clazz) {
        List<Field> list = new ArrayList<Field>();
        Field[] fields;
        do {
            fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                list.add(fields[i]);
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        return list;
    }

    /**
     * 获取表字段名
     *
     * @param clazz
     * @param name
     * @return
     */
    private static String getTableFieldName(Class<?> clazz, String name) {
        try {
            //如果字段加注解了@TableField(exist = false),不走DB查询
            Field field = null;
            try {
                field = clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                //e.printStackTrace();
            }

            //如果为空，则去父类查找字段
            if (field == null) {
                List<Field> allFields = getClassFields(clazz);
                List<Field> searchFields = allFields.stream().filter(a -> a.getName().equals(name)).collect(Collectors.toList());
                if (searchFields != null && searchFields.size() > 0) {
                    field = searchFields.get(0);
                }
            }

            if (field != null) {
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null) {
                    if (tableField.exist() == false) {
                        //如果设置了TableField false 这个字段不需要处理
                        return null;
                    } else {
                        String column = tableField.value();
                        //如果设置了TableField value 这个字段是实体字段
                        if (!"".equals(column)) {
                            return column;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

}
