package top.lucki.lottery.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lucki.lottery.system.entity.Process;
import top.lucki.lottery.system.mapper.ProcessMapper;
import top.lucki.lottery.system.service.IProcessService;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProcessServiceImpl extends ServiceImpl<ProcessMapper, Process> implements IProcessService {

}
