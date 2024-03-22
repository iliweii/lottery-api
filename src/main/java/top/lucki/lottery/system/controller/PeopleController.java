package top.lucki.lottery.system.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.lucki.lottery.common.api.Result;
import top.lucki.lottery.common.controller.BaseController;
import top.lucki.lottery.common.utils.IpUtils;
import top.lucki.lottery.common.utils.QueryGenerator;
import top.lucki.lottery.system.entity.People;
import top.lucki.lottery.system.entity.Process;
import top.lucki.lottery.system.service.IPeopleService;
import top.lucki.lottery.system.service.IProcessService;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/people")
public class PeopleController extends BaseController<People, IPeopleService> {

    private final IPeopleService peopleService;
    private final IProcessService processService;

    /**
     * 分页列表查询
     *
     * @param people
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping(value = "/list")
    public Result<?> queryPageList(People people,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<People> queryWrapper = QueryGenerator.initQueryWrapper(people, req.getParameterMap());
        queryWrapper.lambda().orderByAsc(People::getId);
        Page<People> page = new Page<People>(pageNo, pageSize);
        IPage<People> pageList = peopleService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param people
     * @return
     */
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody People people) {
        peopleService.save(people);
        return Result.OK("签到成功！", people);
    }

    /**
     * 签到
     *
     * @param people
     * @return
     */
    @PostMapping(value = "/sign")
    public Result<?> sign(@RequestBody People people, HttpServletRequest req) {
        if (StrUtil.isEmpty(people.getPeopleName())) return Result.error("姓名不能为空！");
        People query = peopleService.lambdaQuery().eq(People::getPeopleName, people.getPeopleName()).one();
        if (ObjectUtil.isNotEmpty(query) && ObjectUtil.isNotEmpty(query.getId())) {
            query.setSigninTime(new Date());
            peopleService.updateById(query);
            return Result.OK("您已经签过到了！", query);
        } else {
            people.setSigninTime(new Date()).setCreateIp(IpUtils.getIpAddr(req)).setCreateTime(new Date());
            peopleService.save(people);
            return Result.OK("签到成功！", people);
        }
    }

    /**
     * 编辑
     *
     * @param people
     * @return
     */
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody People people) {
        peopleService.updateById(people);
        return Result.OK("编辑成功!", people);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        peopleService.removeById(id);
        processService.lambdaUpdate().eq(Process::getPeopleId, id).remove();
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch() {
        List<People> list = peopleService.list();
        List<Integer> idList = list.stream().map(People::getId).collect(Collectors.toList());
        this.peopleService.removeByIds(idList);
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        People people = peopleService.getById(id);
        return Result.OK(people);
    }

}
