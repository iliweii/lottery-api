package top.lucki.lottery.system.controller;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.lucki.lottery.common.api.BaseEntity;
import top.lucki.lottery.common.api.Result;
import top.lucki.lottery.common.controller.BaseController;
import top.lucki.lottery.common.utils.QueryGenerator;
import top.lucki.lottery.message.handle.impl.EmailSendMsgHandle;
import top.lucki.lottery.message.template.MailResultTemplate;
import top.lucki.lottery.system.entity.People;
import top.lucki.lottery.system.entity.Process;
import top.lucki.lottery.system.service.IPeopleService;
import top.lucki.lottery.system.service.IProcessService;
import top.lucki.lottery.system.service.LotteryService;
import top.lucki.lottery.ws.WebSocketServer;
import top.lucki.lottery.ws.WsMessage;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/process")
public class ProcessController extends BaseController<Process, IProcessService> {

    private final IProcessService processService;
    private final IPeopleService peopleService;
    private final LotteryService lotteryService;
    private final WebSocketServer webSocketServer;

    /**
     * 分页列表查询
     *
     * @param process
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping(value = "/list")
    public Result<?> queryPageList(Process process,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<Process> queryWrapper = QueryGenerator.initQueryWrapper(process, req.getParameterMap());
        queryWrapper.lambda().orderByAsc(Process::getId);
        Page<Process> page = new Page<Process>(pageNo, pageSize);
        IPage<Process> pageList = processService.page(page, queryWrapper);

        // 如果正在抽奖过程中，则移除列表查询中的中奖者
        if (lotteryService.isCountdownActive() && !pageList.getRecords().isEmpty()) {
            List<Process> processList1 = pageList.getRecords();
            // 移除最后一个
            processList1.remove(processList1.size() - 1);
            pageList.setRecords(processList1);
        }

        return Result.OK(pageList);
    }

    /**
     * 抽奖
     *
     * @return
     */
    @GetMapping(value = "/draw")
    public Result<?> draw() {
        // 判断抽奖间隔
        if (lotteryService.isCountdownActive()) {
            return Result.error("上一个抽奖仍在进行中，请稍等！");
        }

        List<People> peopleList = peopleService.list();
        List<Process> processList = processService.list();
        List<Integer> winnerIdList = processList.stream().map(Process::getPeopleId).collect(Collectors.toList());
        List<People> participantList = peopleList.stream().filter(e -> !winnerIdList.contains(e.getId())).collect(Collectors.toList());
        int total = peopleList.size(); // 总人数
        int winnerNum = processList.size(); // 已获奖人数
        if (total == 0) return Result.error("请至少有一个参与人！");
        else if (total - winnerNum <= 0) return Result.error("已全部抽奖完毕！");
        int drawTime = RandomUtil.randomInt(0, 50) + 1; // 抽奖次数
        if (total - winnerNum == 1) drawTime = 1;
        People theWinner = new People();
        List<String> winners = new LinkedList<>();
        // 随机抽取随机个次数，最后一个随机抽取的为本次获奖者
        for (int i = 0; i < drawTime; i++) {
            theWinner = RandomUtil.randomEle(participantList);
            winners.add(theWinner.getPeopleName());
        }
        Process process = new Process();
        process.setPeopleId(theWinner.getId())
                .setCreateTime(new Date())
                .setProcessTime(drawTime)
                .setProcessTotal(total - winnerNum)
                .setProcessInfo(String.join(",", winners))
                .setProcessRemark(theWinner.getPeopleName());
        processService.save(process);

        // 开始倒计时
        lotteryService.startCountdown(process);

        // 发送邮件
        if (StrUtil.isNotEmpty(theWinner.getEmail()) && Validator.isEmail(theWinner.getEmail())) {
            EmailSendMsgHandle emailHandle = new EmailSendMsgHandle();
            emailHandle.SendMsg(theWinner.getEmail(),
                    StrUtil.format("恭喜您在年会上被抽中啦！"),
                    MailResultTemplate.get(
                            StrUtil.format("恭喜你，{}", theWinner.getPeopleName()),
                            "您在年会上中奖啦！")
            );
        }

        return Result.OK("操作成功！", process);
    }

    /**
     * 通过id查询
     *
     * @return
     */
    @GetMapping(value = "/getNow")
    public Result<?> getNow() {
        List<People> peopleList = peopleService.list();
        List<Process> processList = processService.list();
        int total = peopleList.size(); // 总人数
        int winnerNum = processList.size(); // 已获奖人数
        int noNum = total - winnerNum;
        BaseEntity baseEntity = new BaseEntity();
        baseEntity.addExtParam("total", total);
        baseEntity.addExtParam("winnerNum", winnerNum);
        baseEntity.addExtParam("noNum", noNum);
        return Result.OK(baseEntity);
    }

    /**
     * 添加
     *
     * @param process
     * @return
     */
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody Process process) {
        processService.save(process);
        return Result.OK("添加成功！", process);
    }

    /**
     * 编辑
     *
     * @param process
     * @return
     */
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody Process process) {
        processService.updateById(process);
        return Result.OK("编辑成功!", process);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        processService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch() {
        List<Process> processList = processService.list();
        List<Integer> winnerIdList = processList.stream().map(Process::getId).collect(Collectors.toList());
        this.processService.removeByIds(winnerIdList);

        webSocketServer.sendMessage(new WsMessage("重置抽奖", "重置抽奖"));

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
        Process process = processService.getById(id);
        return Result.OK(process);
    }

}
