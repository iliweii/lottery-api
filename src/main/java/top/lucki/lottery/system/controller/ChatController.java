package top.lucki.lottery.system.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.lucki.lottery.common.api.Result;
import top.lucki.lottery.common.controller.BaseController;
import top.lucki.lottery.common.utils.QueryGenerator;
import top.lucki.lottery.system.entity.Chat;
import top.lucki.lottery.system.entity.People;
import top.lucki.lottery.system.service.IChatService;
import top.lucki.lottery.system.service.IPeopleService;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/chat")
public class ChatController extends BaseController<Chat, IChatService> {

    private final IChatService chatService;
    private final IPeopleService peopleService;

    /**
     * 分页列表查询
     *
     * @param chat
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @GetMapping(value = "/list")
    public Result<?> queryPageList(Chat chat,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<Chat> queryWrapper = QueryGenerator.initQueryWrapper(chat, req.getParameterMap());
        queryWrapper.lambda().orderByDesc(Chat::getId);
        Page<Chat> page = new Page<Chat>(pageNo, pageSize);
        IPage<Chat> pageList = chatService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param chat
     * @return
     */
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody Chat chat) {
        People people = peopleService.getById(chat.getPeopleId());
        chat.setNickname(StrUtil.isNotEmpty(people.getSigninRemark()) ? people.getSigninRemark() : people.getPeopleName());
        chat.setCreateTime(new Date()).setPeopleAvatar(people.getPeopleAvatar());
        chatService.save(chat);
        return Result.OK("添加成功！", chat);
    }

    /**
     * 编辑
     *
     * @param chat
     * @return
     */
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody Chat chat) {
        chatService.updateById(chat);
        return Result.OK("编辑成功!", chat);
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        chatService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @return
     */
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch() {
        List<Chat> chatList = chatService.list();
        List<Integer> idList = chatList.stream().map(Chat::getId).collect(Collectors.toList());
        this.chatService.removeByIds(idList);
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
        Chat chat = chatService.getById(id);
        return Result.OK(chat);
    }

}
