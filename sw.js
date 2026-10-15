const CACHE_NAME="kucukkuyu-kart-v2";
const STATIC_ASSETS=["./","./index.html","./isyerleri.html","./verify.html","./icon-192.png","./icon-512.png"];
self.addEventListener("install",function(event){
  event.waitUntil(
    caches.open(CACHE_NAME).then(function(cache){
      return cache.addAll(STATIC_ASSETS).catch(function(){});
    })
  );
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

  if(
    url.pathname.endsWith("/members.json") ||
    url.pathname.endsWith("/manifest.json") ||
    url.pathname.endsWith("/icon-192.png") ||
    url.pathname.endsWith("/icon-512.png")
  ){
    event.respondWith(fetch(event.request,{cache:"no-store"}));
    return;
  }

  if(event.request.mode==="navigate"){
    event.respondWith(
      fetch(event.request,{cache:"no-store"}).catch(function(){
        return caches.match("./index.html");
      })
    );
    return;
  }

  event.respondWith(
    caches.match(event.request).then(function(cached){
      return cached || fetch(event.request);
    })
  );
});