const CACHE_NAME="kucukkuyu-kart-v1";
const STATIC_ASSETS=["./","./index.html","./isyerleri.html","./verify.html","./manifest.json"];
self.addEventListener("install",function(event){
  event.waitUntil(caches.open(CACHE_NAME).then(function(cache){return cache.addAll(STATIC_ASSETS).catch(function(){});}));
  self.skipWaiting();
});
self.addEventListener("activate",function(event){
  event.waitUntil(
    caches.keys().then(function(keys){
      return Promise.all(keys.filter(function(k){return k!==CACHE_NAME;}).map(function(k){return caches.delete(k);}));
    })
  );
  self.clients.claim();
});
self.addEventListener("fetch",function(event){
  const url=new URL(event.request.url);
  if(url.pathname.endsWith("/members.json")){
    event.respondWith(fetch(event.request));
    return;
  }
  if(event.request.mode==="navigate"){
    event.respondWith(fetch(event.request).catch(function(){return caches.match("./index.html");}));
    return;
  }
  event.respondWith(caches.match(event.request).then(function(cached){return cached||fetch(event.request);}));
});