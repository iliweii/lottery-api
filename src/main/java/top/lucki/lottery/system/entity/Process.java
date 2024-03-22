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

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_process")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Process extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer peopleId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Integer processTime;
    private Integer processTotal;
    private String processInfo;
    private String processRemark;
}
