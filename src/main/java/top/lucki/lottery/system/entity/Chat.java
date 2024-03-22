package top.lucki.lottery.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;
import top.lucki.lottery.common.api.BaseEntity;

import java.util.Date;

@Data
@TableName("t_chat")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Chat extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer peopleId;
    private String nickname;
    private String peopleAvatar;

    @JsonFormat(timezone = "GMT+8", pattern = "MM月dd日 HH时mm分")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String content;
}
