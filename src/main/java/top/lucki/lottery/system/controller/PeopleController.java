package top.lucki.lottery.system.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import top.lucki.lottery.common.api.Result;
import top.lucki.lottery.common.controller.BaseController;
import top.lucki.lottery.common.utils.IpUtils;
import top.lucki.lottery.common.utils.QueryGenerator;
import top.lucki.lottery.system.entity.People;
import top.lucki.lottery.system.entity.Process;
import top.lucki.lottery.system.entity.Roster;
import top.lucki.lottery.system.service.IPeopleService;
import top.lucki.lottery.system.service.IProcessService;
import top.lucki.lottery.system.service.IRosterService;
import top.lucki.lottery.system.vo.RosterVO;

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
    private final IRosterService rosterService;

    @Value("spring.profiles.active")
    private String activeProfile;

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

    @GetMapping(value = "/roster")
    public Result<?> queryPageList(RosterVO roster,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<RosterVO> queryWrapper = QueryGenerator.initQueryWrapper(roster, req.getParameterMap());
        queryWrapper.lambda().orderByAsc(RosterVO::getId);
        Page<RosterVO> page = new Page<RosterVO>(pageNo, pageSize);
        IPage<RosterVO> pageList = rosterService.pageVO(page, queryWrapper);
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

        // 不能早于 2025年1月10日 18:00
        Date targetDate = DateUtil.parse("2025-01-10 18:00", "yyyy-MM-dd HH:mm");
        if (DateUtil.date().before(targetDate) && StrUtil.equals(activeProfile, "prod")) {
            return Result.error("时间还很早，不着急签到哦！");
        }

        int startProcess = processService.count();
        if (startProcess > 0) {
            return Result.error(501, "抽奖已经开始，不能再签到啦！");
        }

        if (StrUtil.isEmpty(people.getPeopleName())) return Result.error("姓名不能为空！");
        People query = peopleService.lambdaQuery().eq(People::getPeopleName, people.getPeopleName()).one();
        Roster roster = rosterService.getByUsername(people.getPeopleName());

        if (ObjectUtil.isNotEmpty(query) && ObjectUtil.isNotEmpty(query.getId())) {
            query.setSigninTime(new Date());
            peopleService.updateById(query);
            return Result.OK("您已经签过到了！", query);
        } else if (ObjectUtil.notEqual(people.getConfirm(), 1) && ObjectUtil.isNull(roster)) {
            // 判断不存在于名单中
            people.setConfirm(0);
            Result<Object> error = Result.error("名单中暂不存在" + people.getPeopleName() + "，请确认是否签到！");
            error.setResult(people);
            return error;
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

        int startProcess = processService.count();
        if (startProcess > 0) {
            people.addExtParam("startFlag", true);
        }

        return Result.OK(people);
    }

}
