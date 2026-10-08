#!/usr/bin/env python3
"""v1.3.0: transplant the v6.11.7 mobile visual contract into the frozen offline runtime.

Clinical JavaScript, questions, calculation formulas, auth, and content remain untouched.
Native Android owns the fixed bottom dock; do not create a second WebView dock.
"""
import hashlib
import sqlite3
import sys

DB_PATH = sys.argv[1]
STYLE = r"""
/* MEDIPHARM ABG Android v1.3.0 -- WebApp v6.11.7 mobile alignment */
html,body,body.nah-sdp-abg-app-body{
  width:100%!important;max-width:100%!important;margin:0!important;
  overflow-x:clip!important;-webkit-text-size-adjust:100%!important;
}
.nah-abg,.nah-abg.nah-abg--standalone{
  width:100%!important;max-width:100%!important;
  min-width:0!important;margin:0!important;box-sizing:border-box!important;
  overflow:visible!important;isolation:isolate;
}
.nah-abg *,.nah-abg *::before,.nah-abg *::after{box-sizing:border-box}
.nah-abg :is(.nah-abg__workspace-frame,.nah-abg__workspace-panel,
 .nah-abg__app-home--v680,.nah-abg__learn-workspace--v680,
 .nah-abg__canonical-learning,.nah-abg__canonical-cases,
 .nah-abg__v680-panel,.nah-abg__v680-topic-layout){
 max-width:100%!important;min-width:0!important;box-sizing:border-box!important;
}
.nah-abg :is(img,svg,video){max-width:100%!important}
.nah-abg :is(h1,h2,h3,h4,p,strong,small,span,li){overflow-wrap:break-word}
.nah-abg :is(input,select,textarea,button){min-width:0;max-width:100%;font-size:16px}
.nah-abg__mobile-dock,.nah-abg__mobile-dock--v680{display:none!important}
.nah-abg__workspace-frame > .nah-abg__workspace-tabs.nah-abg__app-nav{display:none!important}
.nah-abg > .nah-abg__hero{
 position:sticky!important;top:0!important;z-index:1000!important;
 display:grid!important;grid-template-columns:minmax(0,1fr) auto!important;
 grid-template-areas:"brand actions" "search search"!important;
 align-items:center!important;gap:8px 10px!important;
 width:100%!important;max-width:100%!important;
 min-height:0!important;padding:9px 12px 10px!important;
 border-bottom:1px solid var(--nah-abg-border,#d5e3eb)!important;
 overflow:visible!important;box-shadow:0 2px 10px rgba(22,52,72,.06)!important;
}
.nah-abg > .nah-abg__hero .nah-abg__brand{
 grid-area:brand!important;min-width:0!important;display:flex!important;gap:9px!important;
}
.nah-abg > .nah-abg__hero .nah-abg__brand-copy{min-width:0!important}
.nah-abg > .nah-abg__hero .nah-abg__brand-title{
 font-size:.89rem!important;line-height:1.22!important;max-width:100%!important;
}
.nah-abg > .nah-abg__hero .nah-abg__brand-subtitle{font-size:.68rem!important}
.nah-abg > .nah-abg__hero .nah-abg__logo{
 width:42px!important;height:42px!important;min-width:42px!important;
 padding:0!important;transform:none!important;box-shadow:none!important;border:0!important;
 background:transparent!important;border-radius:13px!important;
}
.nah-abg > .nah-abg__hero .nah-abg__logo img{width:100%!important;height:100%!important;object-fit:contain!important}
.nah-abg > .nah-abg__hero .nah-abg__hero-actions{
 grid-area:actions!important;display:flex!important;justify-self:end!important;
 align-items:center!important;gap:7px!important;min-width:0!important;
}
.nah-abg > .nah-abg__hero :is(.nah-abg__account-toggle,.nah-abg__menu-toggle){
 min-width:42px!important;width:42px!important;height:42px!important;
 min-height:42px!important;padding:0!important;border-radius:12px!important;
}
.nah-abg > .nah-abg__hero .nah-abg__app-search{
 grid-area:search!important;display:block!important;position:relative!important;
 width:100%!important;min-width:0!important;max-width:100%!important;
 margin:0!important;
}
.nah-abg > .nah-abg__hero .nah-abg__app-search-box{
 display:flex!important;width:100%!important;max-width:100%!important;
 min-height:44px!important;border-radius:13px!important;overflow:hidden!important;
}
.nah-abg > .nah-abg__hero .nah-abg__app-search-box input{
 width:100%!important;min-width:0!important;max-width:100%!important;
 font-size:16px!important;text-overflow:ellipsis!important;
}
.nah-abg__app-home--v680,.nah-abg__learn-workspace--v680{
 width:100%!important;max-width:100%!important;
 padding:14px 12px 24px!important;
}
.nah-abg__v680-hero{max-width:100%!important;min-width:0!important}
.nah-abg__v680-page-head{gap:10px!important;flex-wrap:wrap!important}
.nah-abg__v680-page-head h2{font-size:clamp(1.3rem,5vw,1.65rem)!important;line-height:1.3!important}
.nah-abg__v680-page-head p{font-size:.94rem!important;line-height:1.58!important}
.nah-abg__canonical-learning>.nah-abg__v680-learning-tabs{
 display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;
 width:100%!important;max-width:100%!important;min-width:0!important;
 gap:4px!important;padding:4px!important;overflow:visible!important;
}
.nah-abg__canonical-learning>.nah-abg__v680-learning-tabs button{
 min-width:0!important;min-height:48px!important;
 padding:8px 4px!important;font-size:.83rem!important;white-space:normal!important;
}
.nah-abg__canonical-learning [data-canonical-topic-list].nah-abg__v690-list-view,
.nah-abg__canonical-learning .nah-abg__v690-card-list,
.nah-abg__v690-case-list{
 display:grid!important;grid-template-columns:minmax(0,1fr)!important;
 width:100%!important;max-width:100%!important;min-width:0!important;
 gap:12px!important;overflow-x:clip!important;
}
.nah-abg__v690-list-card,.nah-abg__v690-case-card,.nah-abg__dict-card{
 width:100%!important;max-width:100%!important;min-width:0!important;
 min-height:0!important;height:auto!important;margin:0!important;
 grid-template-columns:42px minmax(0,1fr) 18px!important;
 gap:10px!important;padding:14px!important;
}
.nah-abg__dict-card{display:block!important}
.nah-abg__v690-list-card>div,.nah-abg__v690-case-card>div,
.nah-abg__v690-list-card>div>*,.nah-abg__v690-case-card>div>*,
.nah-abg__dict-card>div,.nah-abg__dict-card>div>*{
 min-width:0!important;max-width:100%!important;overflow-wrap:anywhere!important;
}
.nah-abg__v690-list-card :is(strong,p),.nah-abg__v690-case-card :is(strong,p){
 font-size:clamp(.9rem,3.9vw,1rem)!important;line-height:1.48!important;
}
.nah-abg__v690-table-wrap,.nah-abg__v6104-table{
 max-width:100%!important;overflow-x:auto!important;overscroll-behavior-x:contain!important;
}
.nah-abg__v690-table-wrap table{width:max-content!important;min-width:100%!important}
.nah-abg__workspace-panel[data-workspace-panel="analysis"] :is(input,select,textarea){
 width:100%!important;max-width:100%!important;min-width:0!important;
 box-sizing:border-box!important;
}
.nah-abg__workspace-panel[data-workspace-panel="analysis"] :is(.nah-abg__grid--2,.nah-abg__grid--3){
 grid-template-columns:minmax(0,1fr)!important;
}
.nah-abg__viewport-overlay{max-width:100vw!important;max-height:100dvh!important}
.nah-abg__viewport-dialog{max-width:100%!important;max-height:100%!important;min-width:0!important}
.nah-abg__viewport-body{overflow-y:auto!important;overflow-x:auto!important;min-height:0!important}
.nah-abg :is(.nah-abg__form-nav,.nah-abg__app-footer){max-width:100%!important}
@media(max-width:380px){
 .nah-abg__v680-hero{grid-template-columns:minmax(0,1fr)!important}
 .nah-abg__v680-hero-visual{display:none!important}
 .nah-abg > .nah-abg__hero .nah-abg__brand-subtitle{display:none!important}
}
@media(min-width:701px){
 .nah-abg__v690-case-list{grid-template-columns:repeat(2,minmax(0,1fr))!important}
 .nah-abg__canonical-learning .nah-abg__v690-card-list{
  grid-template-columns:repeat(2,minmax(0,1fr))!important}
}
@media(max-height:450px) and (orientation:landscape){
 .nah-abg > .nah-abg__hero{position:relative!important}
}
"""

def main():
    db = sqlite3.connect(DB_PATH)
    meta = dict(db.execute("SELECT key,value FROM app_meta"))
    if meta.get("web_source_version") != "6.11.4":
        raise SystemExit("Unexpected frozen offline dataset source: "+str(meta.get("web_source_version")))
    for key in ("nah-abg.css", "runtime_html"):
        row = db.execute("SELECT content FROM runtime_assets WHERE key=?", (key,)).fetchone()
        if not row:
            raise SystemExit("Missing runtime key: "+key)
        current = row[0].decode("utf-8")
        marker = "MEDIPHARM ABG Android v1.3.0 -- WebApp v6.11.7 mobile alignment"
        if marker in current:
            raise SystemExit("Patch already present: "+key)
        if key == "nah-abg.css":
            updated = current + "\n" + STYLE
        else:
            if "</head>" not in current:
                raise SystemExit("Missing closing head")
            updated = current.replace("</head>", '<style id="abg-android-v130-ui">'+STYLE+'</style></head>', 1)
        payload = updated.encode("utf-8")
        db.execute("UPDATE runtime_assets SET content=?,sha256=? WHERE key=?",
                   (payload, hashlib.sha256(payload).hexdigest(), key))
    db.execute("INSERT OR REPLACE INTO app_meta VALUES('app_version','1.3.0')")
    db.execute("INSERT OR REPLACE INTO app_meta VALUES('data_version','ABG-WEB-6.11.4-UI-6.11.7-ANDR-1.3.0')")
    if db.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
        raise SystemExit("SQLite integrity check failed")
    db.commit()
    check = dict(db.execute("SELECT key,value FROM app_meta"))
    assert check["app_version"] == "1.3.0"
    db.close()
    print("ANDROID_V130_UI_PATCH_OK: clinical runtime unchanged, one native dock")

if __name__ == "__main__":
    main()
