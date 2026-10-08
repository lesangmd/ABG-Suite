#!/usr/bin/env python3
import sys,sqlite3,hashlib
db=sqlite3.connect(sys.argv[1])
css="""/* Android's persistent native dock lives outside the scrolling WebView. */
.nah-abg__mobile-dock,.nah-abg__mobile-dock--v680{display:none!important}
html,body,body.nah-sdp-abg-app-body{width:100%!important;max-width:100%!important;overflow-x:clip!important}
.nah-abg,.nah-abg__workspace-frame,.nah-abg__app-home--v680,.nah-abg__canonical-learning,[data-canonical-workspace]{max-width:100%!important;min-width:0!important}
.nah-abg img,.nah-abg svg{max-width:100%}
.nah-abg__viewport-overlay{max-width:100vw!important;max-height:100dvh!important;box-sizing:border-box!important}
.nah-abg__viewport-dialog{max-width:100%!important;max-height:100%!important;min-width:0!important;box-sizing:border-box!important;overflow:hidden!important}
.nah-abg__viewport-body{min-height:0!important;overflow-y:auto!important;overflow-x:auto!important;overscroll-behavior:contain!important}
@media(max-width:900px){.nah-abg__viewport-overlay{inset:6px!important;width:auto!important;height:auto!important}.nah-abg__viewport-dialog{width:100%!important;max-width:100%!important}.nah-abg__v680-hero{max-width:100%!important}}
@media(max-width:600px){.nah-abg__viewport-overlay{inset:0!important}.nah-abg__viewport-dialog{border-radius:0!important;min-height:100%!important;max-height:100%!important}.nah-abg__viewport-bar{flex-wrap:wrap!important;max-width:100%!important}.nah-abg__viewport-bar>*{min-width:0!important}.nah-abg__viewport-body{padding:12px!important}}
@media(max-width:380px){.nah-abg__v680-hero{grid-template-columns:minmax(0,1fr)!important}.nah-abg__v680-hero-visual{display:none!important}}
@media(max-height:450px) and (orientation:landscape){.nah-abg__viewport-overlay{inset:0!important}.nah-abg__viewport-dialog{border-radius:0!important}}
"""
meta=dict(db.execute("select key,value from app_meta"))
assert meta['web_source_version']=='6.11.4',meta
for key in ('nah-abg.css','runtime_html'):
    raw=db.execute('select content from runtime_assets where key=?',(key,)).fetchone()[0].decode()
    if 'persistent native dock lives outside' not in raw:
        raw=(raw+'\n'+css) if key=='nah-abg.css' else raw.replace('</head>','<style>'+css+'</style></head>',1)
    b=raw.encode()
    assert db.execute('update runtime_assets set content=?,sha256=? where key=?',(b,hashlib.sha256(b).hexdigest(),key)).rowcount==1
db.execute("insert or replace into app_meta values('app_version','1.2.0')")
db.execute("insert or replace into app_meta values('data_version','ABG-WEB-6.11.4-ANDR-1.2.0')")
assert db.execute('pragma integrity_check').fetchone()[0]=='ok'
db.commit()
print('ANDROID_RUNTIME_V120_PATCH_OK')
