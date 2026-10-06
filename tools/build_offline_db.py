#!/usr/bin/env python3
import argparse, hashlib, html, json, os, re, sqlite3, urllib.parse, urllib.request
from datetime import datetime, timezone

DEFAULT_URL='https://www.sachyhoc.com/phan-tich-khi-mau-app/'
DATA_VERSION='ABG-WEB-6.7.3-2026.10.06'
APP_VERSION='1.5.0'
UA='Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/154 Mobile Safari/537.36 MEDIPHARMABGAndroid/1.5.0'
PRODUCTION_JSON=['CASE-BANK-v5.2.json','MIXED-FORMAT-BANK-v5.3.json','MOCK-TEST-PACKS-v5.9.json','BLUEPRINT-EXAM-GENERATOR-v5.5.json','LONGITUDINAL-ASSESSMENT-SCHEMA-v5.8.json','LEARNING-NAVIGATION-v6.2.json','PLATFORM-MANIFEST-v6.0.json','CONTENT-GOVERNANCE-v5.9.json','ADVANCED-CONTENT-MANIFEST-v5.6.json','ASSESSMENT-BANK-v5.1-MANIFEST.json','CASE-BANK-v5.2-MANIFEST.json','MIXED-FORMAT-BANK-v5.3-MANIFEST.json']

def fetch(url,timeout=30,optional=False):
    req=urllib.request.Request(url,headers={'User-Agent':UA,'Accept':'text/html,application/xhtml+xml,application/json,text/css,*/*;q=0.8','Cache-Control':'no-cache'})
    try:
        with urllib.request.urlopen(req,timeout=timeout) as r:return r.read()
    except Exception:
        if optional:return b''
        raise

def attrs(tag):
    return {k.lower():html.unescape(v) for k,_,v in re.findall(r'([\w:-]+)\s*=\s*([\"\'])(.*?)\2',tag,re.S)}

def find_asset_url(page,kind):
    tags=re.findall(r'<link\b[^>]*>',page,re.I|re.S) if kind=='css' else re.findall(r'<script\b[^>]*>',page,re.I|re.S)
    wanted='medipharm-abg-suite-style' if kind=='css' else 'medipharm-abg-suite-'+kind
    for tag in tags:
        a=attrs(tag)
        if wanted in a.get('id',''):
            url=a.get('href') if kind=='css' else a.get('src')
            if url:return url
    raise RuntimeError('Cannot locate '+kind+' asset URL')

def patch_main(main):
    main=re.sub(r'data-abg-member-access="[01]"','data-abg-member-access="1"',main)
    main=re.sub(r'data-abg-trial-remaining="\d+"','data-abg-trial-remaining="999"',main)
    account='<a class="nah-abg__menu-item" href="medipharmabg://account" data-android-account><span class="nah-abg__menu-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 21c.8-4.2 3.5-6 8-6s7.2 1.8 8 6"/></svg></span><span>Tài khoản</span></a>'
    logout='<a class="nah-abg__menu-item" href="medipharmabg://logout" data-android-logout><span class="nah-abg__menu-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M10 4H5v16h5M14 8l4 4-4 4M8 12h10"/></svg></span><span>Đăng xuất</span></a>'
    main=re.sub(r'<a\s+class="nah-abg__menu-item"\s+href="medipharmabg://login".*?</a>',account,main,count=1,flags=re.S)
    main=re.sub(r'<a\s+class="nah-abg__menu-item"\s+href="medipharmabg://register".*?</a>',logout,main,count=1,flags=re.S)
    if 'medipharmabg://account' not in main:
        marker='<button type="button" class="nah-abg__menu-item" data-abg-info="about">'
        extra=account+logout+'<a class="nah-abg__menu-item" href="medipharmabg://check-update"><span class="nah-abg__menu-icon" aria-hidden="true">↻</span><span>Kiểm tra cập nhật</span></a>'
        main=main.replace(marker,extra+marker,1)
    main=re.sub(r'<a\s+class="nah-abg__menu-item"\s+href="https://www\.sachyhoc\.com/?".*?>.*?Về Trang chủ.*?</a>','',main,flags=re.S)
    main=re.sub(r'<button\s+type="button"\s+class="nah-abg__menu-item"\s+data-install-app.*?</button>','',main,flags=re.S)
    main=re.sub(r'<a\s+class="nah-abg__menu-item"[^>]+data-android-download.*?</a>','',main,flags=re.S)
    main=re.sub(r'<div class="nah-abg__notice nah-abg__notice--app"[^>]*>.*?</div>','',main,flags=re.S)
    main=re.sub(r'<div class="nah-abg__footer-access">.*?</div>\s*(?:<p class="nah-abg__footer-trial">.*?</p>)?','<div class="nah-abg__footer-access"><span class="nah-abg__footer-member">Tài khoản đã xác thực · Chế độ ngoại tuyến</span></div>',main,count=1,flags=re.S)
    return main

def build_runtime(page,css,core,ui):
    m=re.search(r'<main\b[^>]*>(.*?)</main>',page,re.I|re.S)
    if not m:raise RuntimeError('Cannot extract <main>')
    main=patch_main(m.group(1))
    cfg={'version':'6.7.3-offline','manifestUrl':'','swUrl':'','appUrl':'https://app.medipharm.local/','isHttps':False}
    access={'member':True,'limit':999,'remaining':999,'exhausted':False,'ajaxUrl':'','nonce':'','registerUrl':'','openLogin':False}
    bridge="(function(){document.documentElement.classList.add('medipharm-abg-offline');window.MEDIPHARMABG_OFFLINE={appVersion:'1.5.0',dataVersion:'ABG-WEB-6.7.3-2026.10.06'};document.addEventListener('click',function(e){var a=e.target.closest&&e.target.closest('a[href^=\"https://www.sachyhoc.com\"],a[href^=\"http://www.sachyhoc.com\"]');if(a){e.preventDefault();}},true);})();"
    return '<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover"><meta name="theme-color" content="#0b6674"><title>Khí Máu</title><style>'+css+'</style></head><body class="nah-sdp-abg-app-body"><main>'+main+'</main><script>window.NAHABGConfig='+json.dumps(cfg,ensure_ascii=False)+';window.NAHABGAccess='+json.dumps(access,ensure_ascii=False)+';</script><script>'+core+'</script><script>'+ui+'</script><script>'+bridge+'</script></body></html>'

def plugin_base(css_url):
    clean=css_url.split('?',1)[0]; marker='/assets/css/'
    return (clean.split(marker,1)[0].rstrip('/')+'/') if marker in clean else clean.rsplit('/',1)[0]+'/'

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--url',default=DEFAULT_URL);ap.add_argument('--output',required=True);args=ap.parse_args()
    page=fetch(args.url).decode('utf-8','replace')
    css_url=urllib.parse.urljoin(args.url,find_asset_url(page,'css'));core_url=urllib.parse.urljoin(args.url,find_asset_url(page,'core'));ui_url=urllib.parse.urljoin(args.url,find_asset_url(page,'ui'))
    css=fetch(css_url).decode('utf-8','replace');core=fetch(core_url).decode('utf-8','replace');ui=fetch(ui_url).decode('utf-8','replace');runtime=build_runtime(page,css,core,ui)
    out=os.path.abspath(args.output);os.makedirs(os.path.dirname(out),exist_ok=True)
    if os.path.exists(out):os.remove(out)
    db=sqlite3.connect(out);db.execute('CREATE TABLE app_meta (key TEXT PRIMARY KEY,value TEXT NOT NULL)');db.execute('CREATE TABLE runtime_assets (key TEXT PRIMARY KEY,mime_type TEXT NOT NULL,content BLOB NOT NULL,sha256 TEXT NOT NULL)');db.execute('CREATE TABLE content_files (path TEXT PRIMARY KEY,mime_type TEXT NOT NULL,content BLOB NOT NULL,sha256 TEXT NOT NULL)');db.execute('CREATE TABLE schema_info (schema_version INTEGER NOT NULL,generated_at TEXT NOT NULL)')
    generated=datetime.now(timezone.utc).isoformat();db.execute('INSERT INTO schema_info VALUES (?,?)',(1,generated))
    meta={'app_version':APP_VERSION,'data_version':DATA_VERSION,'web_source_url':args.url,'web_source_css':css_url,'generated_at':generated,'runtime_mode':'sqlite-self-contained-html','network_policy':'online-auth-and-update-only'}
    db.executemany('INSERT INTO app_meta(key,value) VALUES (?,?)',meta.items())
    rb=runtime.encode();db.execute('INSERT INTO runtime_assets VALUES (?,?,?,?)',('runtime_html','text/html; charset=utf-8',rb,hashlib.sha256(rb).hexdigest()))
    for key,mime,data in [('nah-abg.css','text/css; charset=utf-8',css.encode()),('nah-abg-core.js','application/javascript; charset=utf-8',core.encode()),('nah-abg-ui.js','application/javascript; charset=utf-8',ui.encode())]:db.execute('INSERT INTO runtime_assets VALUES (?,?,?,?)',(key,mime,data,hashlib.sha256(data).hexdigest()))
    base=plugin_base(css_url)
    for name in PRODUCTION_JSON:
        data=fetch(urllib.parse.urljoin(base,name),optional=True)
        if data:db.execute('INSERT OR REPLACE INTO content_files VALUES (?,?,?,?)',(name,'application/json',data,hashlib.sha256(data).hexdigest()))
    db.commit();db.execute('VACUUM');db.close()
    with open(out,'rb') as f:digest=hashlib.sha256(f.read()).hexdigest()
    print(json.dumps({'output':out,'bytes':os.path.getsize(out),'sha256':digest,'runtime_bytes':len(rb),'data_version':DATA_VERSION,'source':args.url},ensure_ascii=False))

if __name__=='__main__':main()
