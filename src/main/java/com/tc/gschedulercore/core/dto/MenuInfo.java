package com.tc.gschedulercore.core.dto;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单信息
 *
 * @author honggang.liu
 */
public class MenuInfo implements Serializable {
    /**
     * 角色名称，只允许英文
     */
    private String name;
    /**
     * 访问的URL
     */
    private String href;
    /**
     * 标题
     */
    private String title;
    /**
     * 图标
     */
    private String icon;

    /**
     * 图片
     */
    private String image;

    /**
     * target
     */
    private String target;

    /**
     * 父菜单
     */
    private String parent;

    private List<MenuInfo> child;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<MenuInfo> getChild() {
        return child;
    }

    public void setChild(List<MenuInfo> child) {
        this.child = child;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

//    public static List<MenuInfo> getChild(String name, List<MenuInfo> menus) {
//        if (CollectionUtils.isEmpty(menus) || StringUtils.isEmpty(name)) {
//            return Collections.emptyList();
//        }
//        List<MenuInfo> childMenu = new ArrayList<>();
//        for (MenuInfo jobMenu : menus) {
//            if (name.equals(jobMenu.getParent())) {
//                childMenu.add(jobMenu);
//            }
//        }
//        return childMenu;
//    }

    /**
     * 两层循环实现建树
     *
     * @param menus 传入的树节点列表
     * @return 树
     */
    public static List<MenuInfo> buildTree(List<MenuInfo> menus) {
        List<MenuInfo> trees = new ArrayList<>();
        for (MenuInfo treeNode : menus) {
            if (StringUtils.isEmpty(treeNode.getParent())) {
                trees.add(treeNode);
            }
            for (MenuInfo it : menus) {
                if (!StringUtils.isEmpty(it.getParent()) && it.getParent().equals(treeNode.getName())) {
                    if (treeNode.getChild() == null) {
                        treeNode.setChild(new ArrayList<>());
                    }
                    treeNode.getChild().add(it);
                }
            }
        }
        return trees;
    }
}
