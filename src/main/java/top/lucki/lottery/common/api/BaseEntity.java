package top.lucki.lottery.common.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 额外参数，不保存在数据库里的，但是需要提供给前端展示的字段可以添加进来
     * <p>(使用{@link JsonAnyGetter}对数据进行扁平化返回给前端)</p>
     */
    @TableField(exist = false)
    private Map<String, Object> extParams;

    @JsonAnyGetter
    public Map<String, Object> getExtParams() {
        return extParams;
    }

    /**
     * 添加额外参数
     *
     * @param column 额外参数名称
     * @param value  参数值
     */
    public void addExtParam(String column, Object value) {
        if (ObjectUtil.isEmpty(extParams)) {
            this.extParams = new HashMap<>();
        }
        extParams.put(column, value);
    }
}
