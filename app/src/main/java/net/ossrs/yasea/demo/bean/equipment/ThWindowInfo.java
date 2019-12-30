package net.ossrs.yasea.demo.bean.equipment;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ThWindowInfo {

    /**
     * 主键唯一编码
     */
    private String id ;
    /**
     * 窗口编号（HWD0101湖北武汉东湖高新01项目01窗口）
     */
    private String code ;
    /**
     * 窗口名称
     */
    private String name ;
    /**
     * 所属项目
     */
    private String projectId ;
    /**
     * 排序号
     */
    private Integer orderNo ;

    /**
     * 创建时间
     */
    private Date createTime ;
    /**
     * 创建人
     */
    private String createBy ;
    /**
     * 修改时间
     */
    private Date updateTime ;
    /**
     * 修改人
     */
    private String updateBy ;

}
