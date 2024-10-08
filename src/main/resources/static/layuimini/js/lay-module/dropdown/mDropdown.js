/**
 * layui_dropdown
 * v2.3.5
 * by Microanswer
 * http://layuidropdown.microanswer.cn/
 **/
Array.isArray || (Array.isArray = function (t) {
    return "[object Array]" === Object.prototype.toString.call(t)
}), String.prototype.trim || (String.prototype.trim = function () {
    return this.replace(/(^\s*)|(\s*$)/g, "")
}), Function.prototype.bind || (Function.prototype.bind = function (t) {
    if ("function" != typeof this) throw new Error("Function.prototype.bind - what is trying to be bound is not callable");

    function n() {
    }

    function i() {
        return e.apply(this instanceof n && t ? this : t, o.concat(Array.prototype.slice.call(arguments)))
    }

    var o = Array.prototype.slice.call(arguments, 1), e = this;
    return n.prototype = this.prototype, i.prototype = new n, i
}), layui.define(["jquery", "laytpl"], function (t) {
    function i(t, n) {
        var i = e[t] || [];
        i.push(n), e[t] = i
    }

    function d(t, i) {
        var n = e[t] || [];
        a.each(n, function (t, n) {
            n(i)
        })
    }

    var a = layui.jquery || layui.$, l = layui.laytpl, o = "a", p = "b", e = {}, c = "1", h = "2", m = "3";

    function f(t) {
        if (!t) throw new Error("菜单条目内必须填写内容。");
        if ("hr" === t) return "hr";
        if (0 !== t.indexOf("{")) throw new Error("除了分割线hr，别的菜单条目都必须保证是合格的Javascript对象或json对象。");
        return new Function("return " + t)()
    }

    function u(t) {
        for (var n = 0; n < t.length; n++) void 0 !== t[n] && null !== t[n] || (t.splice(n, 1), n--)
    }

    function w(t) {
        if (t && 0 < t.length) {
            for (var n = 0, i = [], o = 0; o < t.length; o++) for (var e = t[o], s = 0; s < e.length; s++) e[s].header && e[s].fixed && (n++, i.push(e[s]), e.splice(s, 1), s--);
            if (0 < n) return i
        }
        return null
    }

    var y = window.MICROANSWER_DROPDOWAN || "dropdown",
        v = "<div tabindex='0' class='layui-anim layui-anim-upbit layu-dropdown-root' " + y + "-id='{{d.downid}}' style='display: none;z-index: {{d.zIndex}}'>{{# if (d.arrow){ }}<div class='layu-dropdown-pointer'></div>{{# } }}<div class='layu-dropdown-content' style='margin: {{d.gap}}px {{d.gap}}px;background-color: {{d.backgroundColor}};min-width: {{d.minWidth}}px;max-width: {{d.maxWidth}}px;min-height: {{d.minHeight}}px;max-height: {{d.maxHeight}}px;white-space: {{d.nowrap?\"nowrap\":\"normal\"}}'>",
        g = "</div></div>",
        x = v + "<div class='layu-dropdown-content-table' cellpadding='0' cellspacing='0'>{{# if (d.fixHeaders && d.fixHeaders.length > 0){ }}<div class='layu-dropdown-content-thead'><div class='layu-dropdown-content-tr'>{{# layui.each(d.fixHeaders, function(i, fixHeader){ }}{{# if (fixHeader) { }}<div class='layu-dropdown-content-th'><div class='layu-dropdown-menu-fixed-head {{(d.menuSplitor && i < (d.menus.length-1))?\"layu-menu-splitor\":\"\"}}'><div class='layu-menu-fixed-head' style='text-align: {{fixHeader.align||\"center\"}}'>{{fixHeader.header}}</div></div></div>{{# } else { }}<th><div class='layu-dropdown-menu-fixed-head {{(d.menuSplitor && i < (d.menus.length-1))?\"layu-menu-splitor\":\"\"}}'><div class='layu-menu-fixed-head'>&nbsp;</div></div></th>{{# } }}{{# }); }}</div></div>{{# } }}<div class='layu-dropdown-content-tbody'><div class='layu-dropdown-content-tr'>{{# layui.each(d.menus, function(i, menu){ }}<div class='layu-dropdown-content-td' valign='top'><div class='layu-dropdown-menu-wrap {{(d.menuSplitor && i < (d.menus.length-1))?\"layu-menu-splitor\":\"\"}} layu-overflowauto' style='min-height: {{d.minHeight}}px;max-height: {{d.maxHeight - ((d.fixHeaders)?24:0)}}px;'><ul class='layu-dropdown-menu' dropdown-menu-index='{{i}}' style=''>{{# layui.each(menu, function(index, item){ }}<li class='layu-menu-item-wrap {{(d.fixHeaders && d.fixHeaders.length) > 0?\"layu-nomargin\":\"\"}}'>{{# if ('hr' === item) { }}<hr>{{# } else if (item.header) { }}{{# if (item.withLine) { }}<fieldset class=\"layui-elem-field layui-field-title layu-menu-header layu-withLine\"><legend>{{item.header}}</legend></fieldset>{{# } else { }}<div class='layu-menu-header' style='text-align: {{item.align||\"left\"}}'>{{item.header}}</div>{{# } }}{{# } else { }}<div class='layu-menu-item' dropdown-menu-item-index='{{index}}'><a href='javascript:;' lay-event='{{item.event}}'>{{# if (item.layIcon){ }}<i class='layui-icon {{item.layIcon}}'></i>&nbsp;{{# } }}<span class='{{item.txtClass||\"\"}}'>{{item.txt}}</span></a></div>{{# } }}</li>{{# }); }}</ul></div></div>{{#});}}</div></div></div>" + g,
        $ = {
            menus: void 0,
            templateMenu: "",
            templateMenuStr: "",
            template: "",
            showBy: "click",
            align: "left",
            minWidth: 76,
            maxWidth: 500,
            minHeight: 10,
            maxHeight: 400,
            zIndex: 891,
            gap: 8,
            onHide: function (t, n) {
            },
            onShow: function (t, n) {
            },
            onItemClick: function (t, n) {
            },
            scrollBehavior: "follow",
            backgroundColor: "#FFF",
            cssLink: "https://cdn.jsdelivr.net/gh/microanswer/layui_dropdown@2.3.5/dist/dropdown.css",
            immed: !1,
            arrow: !0,
            templateMenuSptor: "[]",
            menuSplitor: !0,
            appendTo: "next"
        };

    function s(t) {
        "string" == typeof t && (t = a(t)), this.$dom = t, this.systemListeners = {}
    }

    function r(t, e) {
        a(t || "[lay-" + y + "]").each(function () {
            var t = a(this), n = new Function("return " + (t.attr("lay-" + y) || "{}"))();
            t.removeAttr("lay-" + y);
            var i = a.extend({}, n, e || {}), o = t.data(y) || new s(t);
            t.data(y, o), o.init(i)
        })
    }

    s.prototype.onMenuLaytplEnd = function (t) {
        var n = this;
        n.downHtml = t, n.initEvent(), n.option.immed && n.downHtml && n.show()
    }, s.prototype.init = function (t) {
        var n = this;
        if (n.fcd = !1, n.mic = !1, n.opened = !1, n.option ? n.option = a.extend(n.option, t || {}) : n.option = a.extend({
            downid: String(Math.random()).split(".")[1],
            filter: n.$dom.attr("lay-filter")
        }, $, t), 20 < n.option.gap && (n.option.gap = 20), n.$down && (n.$down.remove(), n.$down = void 0), n.option.menus) {
            if (u(n.option.menus), 0 < n.option.menus.length) {
                var i = n.option.menus[0];
                Array.isArray(i) || (n.option.menus = [n.option.menus]);
                for (var o = 0; o < n.option.menus.length; o++) u(n.option.menus[o]);
                n.option.fixHeaders = w(n.option.menus), n.option.nowrap = !0, l(x).render(n.option, function (t) {
                    n.onMenuLaytplEnd(t)
                })
            }
        } else if (n.option.templateMenu || n.option.templateMenuStr) {
            var e, s;
            if (n.option.templateMenu) s = -1 === n.option.templateMenu.indexOf("#") ? "#" + n.option.templateMenu : n.option.templateMenu, e = a(s).html(); else n.option.templateMenuStr && (e = n.option.templateMenuStr);
            var r = a.extend(a.extend({}, n.option), n.option.data || {});
            l(e).render(r, function (t) {
                n.option.menus = function (t, n) {
                    if (!t) return "";
                    if (!n) throw new Error("请指定菜单模板限定符。");
                    for (var i, o, e = n.charAt(0), s = n.charAt(1), r = t.length, d = 0, a = c, l = !1, p = []; d < r;) {
                        var u = t.charAt(d);
                        a !== c || l ? a !== h || l ? a === m && (l ? (o.srcStr += u, l = !1) : "\\" === u ? l = !0 : u === s ? (o = f(o.srcStr), i.push(o), a = h) : o.srcStr += u) : e === u ? (o = {srcStr: ""}, a = m) : s === u && (a = c) : e === u && (i = [], p.push(i), a = h), d += 1
                    }
                    return p
                }(t, n.option.templateMenuSptor), n.option.fixHeaders = w(n.option.menus), n.option.nowrap = !0, l(x).render(n.option, function (t) {
                    n.onMenuLaytplEnd(t)
                })
            })
        } else if (n.option.template) {
            var d;
            d = -1 === n.option.template.indexOf("#") ? "#" + n.option.template : n.option.template, (r = a.extend(a.extend({}, n.option), n.option.data || {})).nowrap = !1, l(v + a(d).html() + g).render(r, function (t) {
                n.onMenuLaytplEnd(t)
            })
        } else layui.hint().error("下拉框目前即没配置菜单项，也没配置下拉模板。[#" + (n.$dom.attr("id") || "") + ",filter=" + n.option.filter + "]")
    }, s.prototype.initSize = function () {
        if (this.$down && (this.$down.find(".layu-dropdown-pointer").css({
            width: 2 * this.option.gap,
            height: 2 * this.option.gap
        }), !this._sized)) {
            var t = 0;
            this.$down.find(".dropdown-menu-wrap").each(function () {
                t = Math.max(t, a(this).height())
            }), this.$down.find(".dropdown-menu-wrap").css({height: t}), this._sized = !0
        }
    }, s.prototype.initPosition = function () {
        if (this.$down) {
            var t, n, i, o,
                e = "number" == typeof window.pageYOffset ? window.pageYOffset : document.documentElement.scrollTop,
                s = this.$dom.offset(), r = this.$dom.outerHeight(), d = this.$dom.outerWidth(), a = s.left,
                l = s.top - e, p = this.$down.outerHeight(), u = this.$down.outerWidth();
            n = r + l, (t = "right" === this.option.align ? a + d - u + this.option.gap : "center" === this.option.align ? a + (d - u) / 2 : a - this.option.gap) + u >= window.innerWidth && (t = window.innerWidth - u - 2 * this.option.gap), i = t < a ? d < u ? a - t + d / 2 : u / 2 : d < u ? t + (a + d - t) / 2 : u / 2, i -= this.option.gap;
            var c = this.$arrowDom;
            o = -this.option.gap, c.css("left", i), c.css("right", "unset"), n + p >= window.innerHeight ? (n = l - p, o = p - this.option.gap, c.css("top", o).addClass("bottom")) : c.css("top", o).removeClass("bottom"), this.$down.css("left", t), this.$down.css("top", n)
        }
    }, s.prototype.renderDownHtml = function () {
        this.$down = a(this.downHtml);
        var t = this.option.appendTo;
        if ("next" === t) this.$dom.after(this.$down); else if ("before" === t) this.$dom.before(this.$down); else {
            if (0 !== t.indexOf("selector_")) throw new Error("不支持此渲染方式。请填写：next, before, selector_xxx 某一项内容。");
            a(t.substr(t.indexOf("_") + 1)).append(this.$down)
        }
    }, s.prototype.show = function () {
        var t = this, n = !1;
        t.$down || (t.renderDownHtml(), t.$arrowDom = t.$down.find(".layu-dropdown-pointer"), n = !0), t.initPosition(), t.opening = !0, setTimeout(function () {
            t.$down.focus()
        }, 100), t.$down.addClass("layui-show"), t.initSize(), t.opened = !0, n && t.initDropdownEvent(), d(o, t), n && t.onSuccess(), t.option.onShow && t.option.onShow(t.$dom, t.$down)
    }, s.prototype.hide = function () {
        this.opened && (this.fcd = !1, this.$down.removeClass("layui-show"), this.opened = !1, this.option.onHide && this.option.onHide(this.$dom, this.$down))
    }, s.prototype.hideWhenCan = function () {
        this.mic || this.opening || this.fcd || this.hide()
    }, s.prototype.toggle = function () {
        this.opened ? this.hide() : this.show()
    }, s.prototype.onSuccess = function () {
        this.option.success && this.option.success(this.$down)
    }, s.prototype._onScroll = function () {
        var t = this;
        t.opened && ("follow" === this.option.scrollBehavior ? setTimeout(function () {
            t.initPosition()
        }, 10) : this.hide())
    }, s.prototype._onResize = function () {
        this.opened && this.initPosition()
    }, s.prototype.initEvent = function () {
        var n = this;
        n.initEvented || (n.initEvented = !0, i(o, function (t) {
            t !== n && n.hide()
        }), this.systemListeners.scrollListener = n._onScroll.bind(n), this.systemListeners.resizeListener = n._onResize.bind(n), a(window).on("scroll", this.systemListeners.scrollListener), n.$dom.parents().on("scroll", this.systemListeners.scrollListener), a(window).on("resize", this.systemListeners.resizeListener), n.initDomEvent())
    }, s.prototype.initDomEvent = function () {
        var t = this;
        t.$dom.mouseenter(function () {
            t.mic = !0, "hover" === t.option.showBy && (t.fcd = !0, t.show())
        }), t.$dom.mouseleave(function () {
            t.mic = !1
        }), "click" === t.option.showBy && t.$dom.on("click", function () {
            t.fcd = !0, t.toggle()
        }), t.$dom.on("blur", function () {
            t.fcd = !1, t.hideWhenCan()
        })
    }, s.prototype.initDropdownEvent = function () {
        var r = this;
        r.$down.find(".layu-dropdown-menu-wrap").on("mousewheel", function (t) {
            var n = a(this);
            (t = t || window.event).cancelable = !0, t.cancelBubble = !0, t.preventDefault(), t.stopPropagation(), t.stopImmediatePropagation && t.stopImmediatePropagation(), t.returnValue = !1, r.scrolling && n.finish();
            var i = -t.originalEvent.wheelDelta || t.originalEvent.detail;
            0 < i ? (50 < i && (i = 50), r.scrolling = !0, n.animate({scrollTop: n.scrollTop() + i}, {
                duration: 170,
                complete: function () {
                    r.scrolling = !1
                }
            })) : i < 0 ? (i < -50 && (i = -50), r.scrolling = !0, n.animate({scrollTop: n.scrollTop() + i}, {
                duration: 170,
                complete: function () {
                    r.scrolling = !1
                }
            })) : r.scrolling = !1
        }), r.$down.mouseenter(function () {
            r.mic = !0, r.$down.focus()
        }), r.$down.mouseleave(function () {
            r.mic = !1
        }), r.$down.on("blur", function () {
            r.fcd = !1, r.hideWhenCan()
        }), r.$down.on("focus", function () {
            r.opening = !1
        }), r.option.menus && a("[" + y + "-id='" + r.option.downid + "']").on("click", "a", function () {
            var t = a(this), n = (t.attr("lay-event") || "").trim();
            if (n) {
                var i = y + "(" + r.option.filter + ")." + p, o = t.parents("[dropdown-menu-index]"),
                    e = t.parents("[dropdown-menu-item-index]"),
                    s = r.option.menus[parseInt(o.attr("dropdown-menu-index"))][parseInt(e.attr("dropdown-menu-item-index"))];
                r.option.onItemClick && r.option.onItemClick(n, s), d(i, {event: n, data: s}), r.hide()
            } else layui.hint().error("菜单条目[" + this.outerHTML + "]未设置event。")
        })
    }, s.prototype.destroy = function (t) {
        if (this.fcd = !1, this.downHtml = void 0, this.mic = !1, this.opened = !1, a(window).off("scroll", this.systemListeners.scrollListener), this.$dom.parents().off("scroll", this.systemListeners.scrollListener), a(window).off("resize", this.systemListeners.resizeListener), this.$down && this.$down.remove(), this.$dom.removeData(y, null), void 0 === t && (t = !0), !t) {
            var n = y + "(" + this.option.filter + ")." + p;
            delete e[n]
        }
    }, r(), window[y + "_useOwnCss"] || layui.link(window[y + "_cssLink"] || $.cssLink, function () {
    }, y + "_css"), t(y, {
        suite: r, onFilter: function (t, n) {
            i(y + "(" + t + ")." + p, function (t) {
                n && n(t.event, t.data)
            })
        }, hide: function (t) {
            a(t).each(function () {
                var t = a(this).data(y);
                t && t.hide()
            })
        }, show: function (n, i) {
            a(n).each(function () {
                var t = a(this).data(y);
                t ? t.show() : (layui.hint().error("警告：尝试在选择器【" + n + "】上进行下拉框show操作，但此选择器对应的dom并没有初始化下拉框。"), (i = i || {}).immed = !0, r(n, i))
            })
        }, destroy: function (t, n) {
            a(t).each(function () {
                var t = a(this).data(y);
                t && t.destroy(n)
            })
        }, version: "2.3.5"
    })
});

layui.define('mDropdown', function(exports) {
    exports('mDropdown', echarts);
});