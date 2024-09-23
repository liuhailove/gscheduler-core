/**
 * seamiter通用API
 */
layui.define(["jquery"], function (exports) {
    var $ = layui.$;
    var seaFun = {
        isNum: function (num) {
            // isNaN()函数 把空串 空格 以及NUll 按照0来处理 所以先去除，
            if (num === "" || num == null) {
                return false;
            }
            if (isNaN(num)) {
                return false;
            }
            return /(^[1-9]\d*$)/.test(num);
        },
        queryAuthUser: function () {
            var result = null;
            $.ajax({
                type: "POST",
                async: false,
                url: "/gscheduler/check",
                dataType: "json",
                success: function (data) {
                    if (data.code === 200) {
                        result = data.data;
                    }
                }
            });
            return result;
        },
        // query Data方法
        queryData: function (type, url, data) {
            var resultData = null;
            $.ajax({
                type: type,
                async: false,
                url: url,
                data: data,
                contentType: "application/json;charset=utf-8",
                dataType: "json",
                success: function (result) {
                    resultData = result;
                }
            });
            return resultData;
        },
        renderECharts: function (metrics, graphData, dateTimeList, elementId, isAreaStyle) {
            var seriesData = [];
            for (var key in graphData) {
                if (isAreaStyle) {
                    seriesData.push({
                        name: key,
                        type: 'line',
                        stack: '总量',
                        areaStyle: {},
                        label: {
                            normal: {
                                show: true,
                                position: 'top'
                            }
                        },
                        data: graphData[key]
                    });
                } else {
                    seriesData.push({
                        name: key,
                        type: 'line',
                        stack: '总量',
                        label: {
                            normal: {
                                show: true,
                                position: 'top'
                            }
                        },
                        data: graphData[key]
                    });
                }
            }
            var chartsRecords = echarts.init(document.getElementById(elementId), 'walden');
            var optionRecords = {
                title: {},
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'cross',
                        label: {
                            backgroundColor: '#6a7985'
                        }
                    }
                },
                legend: {
                    data: metrics
                },
                toolbox: {
                    feature: {
                        saveAsImage: {}
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                xAxis: [
                    {
                        type: 'category',
                        boundaryGap: false,
                        data: dateTimeList,
                    }
                ],
                yAxis: [
                    {
                        type: 'value'
                    }
                ],
                series: seriesData,
            };
            chartsRecords.setOption(optionRecords);
            return chartsRecords;
        },
        // 获取URL请求路径值
        getQueryString: function (name) {
            var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i');
            var r = window.location.search.substr(1).match(reg);
            if (r != null) {
                return unescape(r[2]);
            }
            return null;
        },
        /**
         * json美化
         *   jsonFormat2(json)这样为格式化代码。
         *   jsonFormat2(json,true)为开启压缩模式
         * @param txt
         * @param compress
         * @returns {string}
         */
        jsonFormat: function (txt, compress) {
            var indentChar = '    ';
            if (/^\s*$/.test(txt)) {
                return txt;
            }
            try {
                var data = eval('(' + txt + ')');
            } catch (e) {
                return txt;
            }
            // var draw = [], last = false, This = this, line = compress ? '' : '\n', nodeCount = 0, maxDepth = 0;
            //
            // var notify = function (name, value, isLast, indent/*缩进*/, formObj) {
            //     nodeCount++;/*节点计数*/
            //     for (var i = 0, tab = ''; i < indent; i++) tab += indentChar;/* 缩进HTML */
            //     tab = compress ? '' : tab;/*压缩模式忽略缩进*/
            //     maxDepth = ++indent;/*缩进递增并记录*/
            //     if (value && value.constructor == Array) {/*处理数组*/
            //         draw.push(tab + (formObj ? ('"' + name + '":') : '') + '[' + line);/*缩进'[' 然后换行*/
            //         for (var i = 0; i < value.length; i++)
            //             notify(i, value[i], i == value.length - 1, indent, false);
            //         draw.push(tab + ']' + (isLast ? line : (',' + line)));/*缩进']'换行,若非尾元素则添加逗号*/
            //     } else if (value && typeof value == 'object') {/*处理对象*/
            //         draw.push(tab + (formObj ? ('"' + name + '":') : '') + '{' + line);/*缩进'{' 然后换行*/
            //         var len = 0, i = 0;
            //         for (var key in value) len++;
            //         for (var key in value) notify(key, value[key], ++i == len, indent, true);
            //         draw.push(tab + '}' + (isLast ? line : (',' + line)));/*缩进'}'换行,若非尾元素则添加逗号*/
            //     } else {
            //         if (typeof value == 'string') value = '"' + value + '"';
            //         draw.push(tab + (formObj ? ('"' + name + '":') : '') + value + (isLast ? '' : ',') + line);
            //     }
            // };
            // var isLast = true, indent = 0;
            // notify('', data, isLast, indent, false);
            // return draw.join('');
            // 设置缩进为2个空格
            return JSON.stringify(JSON.parse(txt), null, 2);
        }

    };

    exports("seaFun", seaFun);
});