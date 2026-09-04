import asyncio, pathlib
from playwright.async_api import async_playwright

S = pathlib.Path("/tmp/claude-0/-home-user-AiLab/94fd8034-0eda-5771-860b-639096809441/scratchpad/banner")

FONTS = """
@font-face{font-family:'NSKR';src:url('noto-sans-kr-korean-400-normal.woff2') format('woff2');font-weight:400}
@font-face{font-family:'NSKR';src:url('noto-sans-kr-korean-700-normal.woff2') format('woff2');font-weight:700}
@font-face{font-family:'NSKR';src:url('noto-sans-kr-korean-900-normal.woff2') format('woff2');font-weight:900}
@font-face{font-family:'NSL';src:url('noto-sans-kr-latin-400-normal.woff2') format('woff2');font-weight:400}
@font-face{font-family:'NSL';src:url('noto-sans-kr-latin-700-normal.woff2') format('woff2');font-weight:700}
@font-face{font-family:'NSL';src:url('noto-sans-kr-latin-900-normal.woff2') format('woff2');font-weight:900}
"""

# 데이터 분포형 히스토그램 (우상향 추세선 없음) + 레이더 호 + 금빛 동전
def motif(w, h, cx, cy, scale=1.0, coin_gap=34):
    bars = [(0,.46),(1,.62),(2,.86),(3,1.0),(4,.78),(5,.55),(6,.34)]
    bw, gap = 22*scale, 14*scale
    total = len(bars)*bw + (len(bars)-1)*gap
    x0 = cx - total/2
    base = cy + 92*scale
    maxh = 150*scale
    rects, strokes = [], []
    for i,(_,f) in enumerate(bars):
        x = x0 + i*(bw+gap); bh = maxh*f; y = base-bh
        rects.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{bw:.1f}" height="{bh:.1f}" rx="3"/>')
        strokes.append(f'<rect x="{x+.5:.1f}" y="{y+.5:.1f}" width="{bw-1:.1f}" height="{bh-1:.1f}" rx="3"/>')
    r = 17.5*scale
    coin_x, coin_y = x0 + 3*(bw+gap) + bw/2, base - maxh - coin_gap*scale
    return f'''<svg class="motif" viewBox="0 0 {w} {h}" fill="none">
<defs>
<linearGradient id="bar" x1="0" y1="1" x2="0" y2="0">
  <stop offset="0" stop-color="#3C74D6" stop-opacity=".10"/>
  <stop offset="1" stop-color="#6FA0EE" stop-opacity=".40"/></linearGradient>
<linearGradient id="gold" x1="0" y1="0" x2="1" y2="1">
  <stop offset="0" stop-color="#FBDD98"/><stop offset=".5" stop-color="#EEBB4E"/>
  <stop offset="1" stop-color="#C9902B"/></linearGradient>
<radialGradient id="glow" cx=".5" cy=".5" r=".5">
  <stop offset="0" stop-color="#F0C566" stop-opacity=".40"/>
  <stop offset="1" stop-color="#F0C566" stop-opacity="0"/></radialGradient>
</defs>
<g stroke="#6E9CE4" stroke-opacity=".15" stroke-width="1">
  <circle cx="{cx}" cy="{cy}" r="{58*scale:.0f}"/><circle cx="{cx}" cy="{cy}" r="{100*scale:.0f}"/>
  <circle cx="{cx}" cy="{cy}" r="{142*scale:.0f}"/><circle cx="{cx}" cy="{cy}" r="{184*scale:.0f}"/>
</g>
<g fill="url(#bar)">{''.join(rects)}</g>
<g stroke="#7FAAF0" stroke-opacity=".28" stroke-width="1.1">{''.join(strokes)}</g>
<line x1="{x0-16*scale:.1f}" y1="{base:.1f}" x2="{x0+total+16*scale:.1f}" y2="{base:.1f}"
  stroke="#7FAAF0" stroke-opacity=".30" stroke-width="1.2"/>
<line x1="{coin_x:.1f}" y1="{coin_y+r:.1f}" x2="{coin_x:.1f}" y2="{base-maxh:.1f}"
  stroke="#F0C566" stroke-opacity=".38" stroke-width="1.4" stroke-dasharray="3 4"/>
<circle cx="{coin_x:.1f}" cy="{coin_y:.1f}" r="{40*scale:.1f}" fill="url(#glow)"/>
<circle cx="{coin_x:.1f}" cy="{coin_y:.1f}" r="{r:.1f}" fill="url(#gold)"/>
<circle cx="{coin_x:.1f}" cy="{coin_y:.1f}" r="{r:.1f}" fill="none" stroke="#FFF0CC" stroke-opacity=".62" stroke-width="1.2"/>
<circle cx="{coin_x:.1f}" cy="{coin_y:.1f}" r="{9*scale:.1f}" fill="none" stroke="#8A5F14" stroke-opacity=".36" stroke-width="1.6"/>
</svg>'''

STATS = [("69,903","건","10년 ELS 전수 판정"),("2","개","직접 만든 금융 서비스"),("매주","","데이터 갱신·기록")]

def stats_html(vs, ls):
    out=[]
    for v,u,l in STATS:
        us = f'<span class="u">{u}</span>' if u else ''
        out.append(f'<div class="st"><span class="sv">{v}{us}</span><span class="sl">{l}</span></div>')
    return f'<div class="stats">{"".join(out)}</div>'

PC = f"""<meta charset="utf-8"><style>{FONTS}
*{{margin:0;padding:0;box-sizing:border-box}}
html,body{{width:966px;height:270px;overflow:hidden}}
body{{font-family:'NSL','NSKR',sans-serif;background:#0A1730;position:relative}}
.bg{{position:absolute;inset:0;background:
 radial-gradient(120% 150% at 84% 50%, rgba(56,116,214,.36) 0%, rgba(56,116,214,0) 60%),
 linear-gradient(104deg,#08132A 0%,#0E2148 54%,#17325F 100%)}}
.motif{{position:absolute;right:-6px;top:0;width:430px;height:270px}}
.wrap{{position:absolute;left:52px;top:0;height:270px;width:560px;
 display:flex;flex-direction:column;justify-content:center}}
.eyebrow{{font-family:'NSKR',sans-serif;font-size:11.5px;font-weight:700;
 letter-spacing:.13em;color:#8FB4EC;margin-bottom:12px}}
h1{{font-family:'NSKR',sans-serif;font-size:38px;font-weight:900;line-height:1.16;
 letter-spacing:-.024em;color:#fff;text-shadow:0 2px 14px rgba(0,0,0,.32)}}
h1 .g{{color:#F0C566}}
.sub{{font-family:'NSKR',sans-serif;font-size:13.5px;font-weight:400;line-height:1.6;
 color:#B9CDEB;margin-top:13px;letter-spacing:-.014em}}
.stats{{display:flex;margin-top:22px;align-items:flex-start}}
.st{{padding-right:24px;margin-right:24px;border-right:1px solid rgba(150,182,232,.24)}}
.st:last-child{{border-right:0;padding-right:0;margin-right:0}}
.sv{{font-family:'NSL','NSKR',sans-serif;font-size:21px;font-weight:900;color:#fff;
 letter-spacing:-.02em;line-height:1;display:block}}
.sv .u{{font-family:'NSKR',sans-serif;font-size:12.5px;font-weight:700;margin-left:2px;color:#DCE7F7}}
.sl{{font-family:'NSKR',sans-serif;font-size:10.5px;font-weight:400;color:#8FA9CF;
 margin-top:7px;display:block;letter-spacing:-.012em}}
</style><div class="bg"></div>{motif(430,270,300,150,1.0)}
<div class="wrap">
<div class="eyebrow">읽고 · 만들고 · 기록합니다</div>
<h1>돈의 <span class="g">구조</span>를 읽고<br>만드는 삶</h1>
<div class="sub">복잡한 돈을 데이터로 풀어 읽고, 그 답을 직접 서비스로 만듭니다.</div>
{stats_html(0,0)}</div>"""

MO = f"""<meta charset="utf-8"><style>{FONTS}
*{{margin:0;padding:0;box-sizing:border-box}}
html,body{{width:1080px;height:1080px;overflow:hidden}}
body{{font-family:'NSL','NSKR',sans-serif;background:#0A1730;position:relative}}
.bg{{position:absolute;inset:0;background:
 radial-gradient(90% 70% at 50% 76%, rgba(56,116,214,.36) 0%, rgba(56,116,214,0) 64%),
 linear-gradient(168deg,#08132A 0%,#0E2148 56%,#17325F 100%)}}
.motif{{position:absolute;left:0;bottom:0;width:1080px;height:420px}}
.wrap{{position:absolute;left:0;top:100px;width:1080px;text-align:center;padding:0 80px}}
.eyebrow{{font-family:'NSKR',sans-serif;font-size:26px;font-weight:700;
 letter-spacing:.16em;color:#8FB4EC;margin-bottom:30px}}
h1{{font-family:'NSKR',sans-serif;font-size:88px;font-weight:900;line-height:1.18;
 letter-spacing:-.026em;color:#fff;text-shadow:0 3px 22px rgba(0,0,0,.34)}}
h1 .g{{color:#F0C566}}
.sub{{font-family:'NSKR',sans-serif;font-size:31px;font-weight:400;line-height:1.58;
 color:#B9CDEB;margin-top:34px;letter-spacing:-.016em}}
.stats{{display:flex;justify-content:center;margin-top:44px;align-items:flex-start}}
.st{{padding-right:38px;margin-right:38px;border-right:1px solid rgba(150,182,232,.24)}}
.st:last-child{{border-right:0;padding-right:0;margin-right:0}}
.sv{{font-family:'NSL','NSKR',sans-serif;font-size:44px;font-weight:900;color:#fff;
 letter-spacing:-.022em;line-height:1;display:block}}
.sv .u{{font-family:'NSKR',sans-serif;font-size:25px;font-weight:700;margin-left:3px;color:#DCE7F7}}
.sl{{font-family:'NSKR',sans-serif;font-size:21px;font-weight:400;color:#8FA9CF;
 margin-top:14px;display:block;letter-spacing:-.014em}}
</style><div class="bg"></div>{motif(1080,420,540,262,1.5,30)}
<div class="wrap">
<div class="eyebrow">읽고 · 만들고 · 기록합니다</div>
<h1>돈의 <span class="g">구조</span>를 읽고<br>만드는 삶</h1>
<div class="sub">복잡한 돈을 데이터로 풀어 읽고,<br>그 답을 직접 서비스로 만듭니다.</div>
{stats_html(0,0)}</div>"""

async def main():
    (S/"pc.html").write_text(PC, encoding="utf-8")
    (S/"mo.html").write_text(MO, encoding="utf-8")
    async with async_playwright() as p:
        b = await p.chromium.launch(
            executable_path="/opt/pw-browsers/chromium-1194/chrome-linux/chrome",
            args=["--no-sandbox"])
        for name,f,w,h in [("blog-cover-pc","pc.html",966,270),
                           ("blog-cover-mobile","mo.html",1080,1080)]:
            pg = await b.new_page(viewport={"width":w,"height":h}, device_scale_factor=2)
            await pg.goto((S/f).as_uri())
            await pg.wait_for_timeout(500)
            await pg.screenshot(path=str(S/f"{name}.png"))
            print(name, "->", w*2, "x", h*2)
            await pg.close()
        await b.close()

asyncio.run(main())
