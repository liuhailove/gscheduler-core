// 获取URL请求路径值
function getQueryString(name) {
    var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i');
    var r = window.location.search.substr(1).match(reg);
    if (r != null) {
        return unescape(r[2]);
    }
    return null;
}

//写cookies
function setCookie(name, value) {
    var Days = 7;
    var exp = new Date();
    exp.setTime(exp.getTime() + Days * 24 * 60 * 60 * 1000);
    document.cookie = name + "=" + escape(value) + ";expires=" + exp.toGMTString();
}

//读取cookies
function getCookie(name) {
    var arr, reg = new RegExp("(^| )" + name + "=([^;]*)(;|$)");
    if (arr = document.cookie.match(reg)) {
        return unescape(arr[2]);
    } else {
        return null;
    }
}

//读取cookies
function getXxlCookie() {
    return getCookie("XXL_JOB_LOGIN_IDENTITY");
}

//删除cookies
function delCookie(name) {
    var exp = new Date();
    exp.setTime(exp.getTime() - 1);
    var cval = getCookie(name);
    if (cval != null) {
        document.cookie = name + "=" + cval + ";expires=" + exp.toGMTString();
    }
}

//获取当前日期
var getNowDate = function () {
    var d = new Date();
    var year = d.getFullYear();
    var month = d.getMonth() + 1;
    var day = d.getDate();
    var dateStr = year + '-' + getFormatDate(month) + '-' + getFormatDate(day);
    return dateStr;
}

//格式化日期的月份或天数的显示（小于10，在前面增加0）
function getFormatDate(value) {
    var result;
    result = value < 10 ? ("0" + value) : value;
    return result;
}

//获取与毫秒数的转化比例（相差天数：1，相差小时数：2，相差分钟数：3，相差秒数：4）
var getDifferScale = function (value) {
    var format;
    //获取转化比（天数跟毫秒数的比例）
    if (value === 1) {
        format = parseFloat(24 * 60 * 60 * 1000);
    }
    //获取转化比（小时数跟毫秒数的比例）
    else if (value === 2) {
        format = parseFloat(60 * 60 * 1000);
    }
    //获取转化比（分钟数跟毫秒数的比例）
    else if (value === 3) {
        format = parseFloat(60 * 1000);
    }
    //获取转化比（秒数跟毫秒数的比例）
    else if (value === 4) {
        format = parseFloat(1000);
    }
    return format;
}

//获取两个日期的相差日期数(differ 相差天数：1、相差小时数：2、相差分钟数：3、相差秒数：4)
var getDifferDate = function (firstDate, secondDate, differ) {
    //1)将两个日期字符串转化为日期对象
    var startDate = new Date(firstDate);
    var endDate = new Date(secondDate);
    //2)计算两个日期相差的毫秒数
    var msecNum = endDate.getTime() - startDate.getTime();
    //3)计算两个日期相差的天数
    return Math.floor(msecNum / getDifferScale(differ));
}
/**
 *对Date的扩展，将 Date 转化为指定格式的String
 *月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
 *年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)
 *例子：
 *(new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423
 *(new Date()).Format("yyyy-M-d h:m:s.S")      ==> 2006-7-2 8:9:4.18
 */
Date.prototype.format = function (fmt) {
    var o = {
        "M+": this.getMonth() + 1, //月份
        "d+": this.getDate(), //日
        "H+": this.getHours(), //小时
        "m+": this.getMinutes(), //分
        "s+": this.getSeconds(), //秒
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度
        "S": this.getMilliseconds() //毫秒
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}
Array.prototype.contains = function (obj) {
    var i = this.length;
    while (i--) {
        if (this[i] === obj) {
            return true;
        }
    }
    return false;
}
// 全局执行器操作权限数据
var jobGroupPermissionOp = [];
// 全局普通操作权限数据
var jobInfoPermissionOp = [];
// 全局用户操作权限数据
var jobUserPermissionOp = [];
// 全局角色操作权限数据
var jobRolePermissionOp = [];
// 全局分组操作权限数据
var jobUGroupPermissionOp = [];
// 全局平台操作权限数据
var jobPlatformPermissionOp = [];
// 全局日志Tag操作权限数据
var jobLogTagPermissionOp = [];