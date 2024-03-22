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
@TableName("t_people")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class People extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String peopleName;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String createIp;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signinTime;
    private String signinType;
    private String signinInfo;
    private String signinRemark;
    private String peopleAvatar;
    private String peopleUserid;
    private String phone;
    private String email;
    private String wechat;
    private String wechatInfo;
    private String otherInfo;
    private String remark;
}
