/**
 * layselect 下拉框插件，只支持单选
 * @author:Darker.Wang
 * @version:1.1（优化增加数据内部控制元素默认选择）
 * render参数{
 *	属性：
 * 	elem：元素ID，带#号必传
 *  url：请求路径的URL，必传
 *  data:请求url所携带的参数，可选
 *  type:请求方式，默认为get，可选
 *  option:元素数据，数组，用于不通过请求url获取数据，本地自动赋值，可选
 *  select:指定选中的索引项，可选
 *  方法：
 *  format:格式化方法映射，将返回的Data元素映射乘标准格式
 *  success:成功回调，返回加载后的对象数组
 *  fail:失败回调，加载失败的处理
 *  onselect:点击选择时事件响应（如事件无响应，记得加lay-filter属性=id）
 * }
 * 请求返回需对象格式：rtvObj=option={status,code,codeName,select,...}，不满足的通过format映射处理 status=0 时表示禁用
 * 其他说明：
 * 1、分组展示按照：groupName，groupChildren 数组 [rtvObj]
 * 2、暂不支持多选（后期版本规划：支持多选，在select标签上设置属性：multiple="multiple"）
 * 3、获取原始数据，可通过调用success得到，返回原始数据的集合
 * 4、点击事件回调，可通过实现onselect 触发，参数为当前点击的值
 * @param exports
 * @returns 返回一个对象
 * 码云地址：https://gitee.com/godbirds/layselect
 */
layui.define(['element','form','jquery'],function(exports){
    var element = layui.element;
    var form = layui.form;
    var $ = layui.jquery;
    var obj={
        //{elem,url,data,type,format}
        render:function(param){
            var that = this;
            var eid = param.elem;
            that.selectValues=new Array();
            if(param.type == null || param.type==undefined){
                param.type = 'get';//默认get请求
            }
            if(param.data){
                param.data = JSON.stringify(data);
            }else{
                param.data = {}
            }
            if(param.url == null || param.url == '' || param.url==undefined){
                if(param.option == null || param.option == undefined){
                    param.option = new Array();//重新定义为了正常显示下拉框
                }
                that._init(eid,param,param.option);
            }else{
                $.ajax({
                    url: param.url,
                    type: param.type,
                    data: param.data,
                    async: 'false',
                    dataType: 'json',
                    headers: {Accept: "application/json; charset=utf-8"},
                    contentType : 'application/json',//指定json头
                    success: function(data){
                        that._init(eid,param,data)
                    },
                    error: function (e) {
                        //加载失败执行回调函数
                        console.log("Ajax request "+param.url+" error! ");
                        if(param.fail){
                            param.fail(e);//加载失败回调函数，请求URL时才会触发
                        }
                    }
                });
            }
            /***
             * 初始化：data=[{code,status,codeName,select,groupName,groupChildren}]
             */
            that._init=function(eid,param,data){
                $(eid).empty();//请求成功时清空
                $(eid).prepend("<option value=''>请选择</option>");//添加第一个option值
                var option = new Array();
                if(param.format){//格式化
                    for (var i = 0; i < data.length; i++) {
                        var tdata = param.format(data[i]);
                        option.push({
                            code:tdata.code,
                            status:tdata.status||'1',
                            select:tdata.select||'false',
                            codeName:tdata.codeName,
                            groupName:tdata.groupName,
                            groupChildren:tdata.groupChildren
                        });
                    }
                }else{//不格式化
                    for (var i = 0; i < data.length; i++) {
                        option.push({
                            code:data[i].code,
                            status:data[i].status||'1',
                            select:data[i].select||'false',
                            codeName:data[i].codeName,
                            groupName:data[i].groupName,
                            groupChildren:data[i].groupChildren
                        });
                    }
                }
                for (var i = 0; i < option.length; i++) {
                    //分组
                    if(option[i].groupChildren != null && option[i].groupChildren != undefined &&
                        option[i].groupChildren != ''   && option[i].groupChildren.length > 0){
                        $(eid).append("<optgroup label='"+option[i].groupName+"'>");
                        option[i].groupChildren.forEach(function(item,index){
                            var status = "";var topborder ="",buttomborder="",checkoff="";
                            if(item.status && item.status == '0'){status = "disabled='disabled'";}//是否有效
                            $(eid).append("<option value='"+item.code+"' "+status+">"+item.codeName+"</option>");
                            //内部数据option中的选中标识优先级高于外部指定的优先级
                            if(item.select != undefined && (item.select == 'true' || item.select == true) && item.status != '0'){
                                that.selectValues.push(item);
                            }else if(param.select != undefined && item.status != '0'){
                                var xs = param.select+"";
                                if(xs.indexOf('-') > -1){
                                    var x = xs.split("-");
                                    if(i == x[0] && index == x[1]){
                                        that.selectValues.push(item);
                                    }
                                }else if(i == param.select){
                                    that.selectValues.push(item);
                                }
                            }//选择指定选中项
                        });
                        $(eid).append("</optgroup>");
                    }else{//不分组
                        var status = "";var topborder ="",buttomborder="",checkoff="";
                        if(option[i].status && option[i].status == '0'){status = "disabled='disabled'";}//是否有效
                        $(eid).append("<option value='"+option[i].code+"' "+status+">"+option[i].codeName+"</option>");
                        //内部数据option中的选中标识优先级高于外部指定的优先级
                        if(option[i].select != undefined && (option[i].select == 'true' || option[i].select == true) && option[i].status != '0'){
                            that.selectValues.push(option[i]);
                        }else if(param.select != undefined && param.select == i && option[i].status != '0'){
                            that.selectValues.push(option[i]);
                        }//选择指定选中项
                    }
                }
                var k = eid.replace('#','');
                var multiple = $(eid).attr('multiple');//多选控制，暂时无用
                if(that.selectValues.length>0){//默认选中数据在循环外部，提高渲染效率
                    that.selectValues.forEach(function(item,index){
                        $(eid).find("option[value = '"+item.code+"']").attr("selected","selected");
                    });
                }else{
                    //$(eid).find("option[value = '']").attr("selected","selected");//默认选中请选择
                }
                //加载成功执行回调函数
                if(param.success){
                    //console.log(k+" call success",option);
                    param.success(option);
                    option = null;
                }
                //监听事件：这里做自己想做的事情
                form.render('select');//select是固定写法 不是选择器
                form.on('select('+eid.replace('#','')+')', function(data){
                    if(param.onselect){//点击选择事件
                        param.onselect(data.value);
                    }
                });
            }
        }
    };
    exports('layselect',obj);
});