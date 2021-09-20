var e=Object.defineProperty,t=Object.defineProperties,a=Object.getOwnPropertyDescriptors,r=Object.getOwnPropertySymbols,l=Object.prototype.hasOwnProperty,o=Object.prototype.propertyIsEnumerable,s=(t,a,r)=>a in t?e(t,a,{enumerable:!0,configurable:!0,writable:!0,value:r}):t[a]=r,n=(e,t)=>{for(var a in t||(t={}))l.call(t,a)&&s(e,a,t[a]);if(r)for(var a of r(t))o.call(t,a)&&s(e,a,t[a]);return e},i=(e,r)=>t(e,a(r));import{r as d,o as u,c,a as m,b as p,w as f,F as g,d as h,e as v,i as y,p as S,s as E,f as N,g as b,h as _,n as T,j as O,k as I,l as D,m as A,q as C,t as L,u as w,v as P,x as R,y as j,z as M,A as k,B as V,C as F,D as x,E as z,G as H,H as $,I as q,J as B}from"./vendor.3379b66a.js";!function(){const e=document.createElement("link").relList;if(!(e&&e.supports&&e.supports("modulepreload"))){for(const e of document.querySelectorAll('link[rel="modulepreload"]'))t(e);new MutationObserver((e=>{for(const a of e)if("childList"===a.type)for(const e of a.addedNodes)"LINK"===e.tagName&&"modulepreload"===e.rel&&t(e)})).observe(document,{childList:!0,subtree:!0})}function t(e){if(e.ep)return;e.ep=!0;const t=function(e){const t={};return e.integrity&&(t.integrity=e.integrity),e.referrerpolicy&&(t.referrerPolicy=e.referrerpolicy),"use-credentials"===e.crossorigin?t.credentials="include":"anonymous"===e.crossorigin?t.credentials="omit":t.credentials="same-origin",t}(e);fetch(e.href,t)}}();const G={},Y={id:"nav"},W=h("Home"),U=h(" | "),J=h("Map");G.render=function(e,t){const a=d("router-link"),r=d("router-view");return u(),c(g,null,[m("div",Y,[p(a,{to:{name:"Home"}},{default:f((()=>[W])),_:1}),U,p(a,{to:{name:"MapNodes"}},{default:f((()=>[J])),_:1})]),p(r)],64)};const Q={},K={class:"home"},X=[m("h4",null,"This is the home page",-1)];Q.render=function(e,t){return u(),c("div",K,X)};const Z={},ee=function(e,t){return t&&0!==t.length?Promise.all(t.map((e=>{if((e=`/${e}`)in Z)return;Z[e]=!0;const t=e.endsWith(".css"),a=t?'[rel="stylesheet"]':"";if(document.querySelector(`link[href="${e}"]${a}`))return;const r=document.createElement("link");return r.rel=t?"stylesheet":"modulepreload",t||(r.as="script",r.crossOrigin=""),r.href=e,document.head.appendChild(r),t?new Promise(((e,t)=>{r.addEventListener("load",e),r.addEventListener("error",t)})):void 0}))).then((()=>e())):e()};const te={name:"MarkerCluster",props:{options:{type:Object,default:()=>({})}},setup(e,t){const a=v({}),r=v(!1),l=y("addLayer"),o=y("removeLayer");S("canSetParentHtml",(()=>!!a.value.getElement())),S("setParentHtml",(e=>a.value.getElement().innerHTML=e)),S("addLayer",(e=>{a.value.addLayer(e.leafletObject)})),S("removeLayer",(e=>{a.value.removeLayer(e.leafletObject)}));const{methods:s}=E(e,a,t);return N((async()=>{const{DomEvent:o,marker:d}=await ee((()=>import("./leaflet-src.esm.291dd8ba.js")),[]),{MarkerClusterGroup:u}=await ee((()=>import("./leaflet.markercluster-src.ff8ce8ef.js").then((function(e){return e.l}))),["assets/leaflet.markercluster-src.ff8ce8ef.js","assets/vendor.3379b66a.js"]);a.value=new u(e.options);const c=b(t.attrs);o.on(a.value,c),_(s,a.value,e),l(i(n(n({},e),s),{leafletObject:a.value})),r.value=!0,T((()=>t.emit("ready",a.value)))})),O((()=>a.value&&a.value._leaflet_id&&o({leafletObject:a.value}))),{ready:r,leafletObject:a}},render(){return I(this.ready,this.$slots)}},ae={style:{display:"none"}};te.render=function(e,t,a,r,l,o){return u(),c("div",ae,[r.ready?D(e.$slots,"default",{key:0}):A("",!0)])},C("data-v-10e222b0");const re={class:"leaflet"},le={class:"geo-map"};L();var oe=w({setup(e){const t=v(7);let a=v(!1),r=v("");v(!1);let l=v();async function o(){await T(),r.value=l.value.leafletObject,a.value=!0,console.log("Calling leafletReady map value",l.value),console.log("Calling leaflet leafletObject",r)}return(e,r)=>(u(),c("div",re,[m("div",le,[p(P(M),{ref:l,"max-zoom":19,modelValue:t.value,"onUpdate:modelValue":r[0]||(r[0]=e=>t.value=e),zoomAnimation:!0,center:{lat:51.289404225298256,lng:9.697202050919614},onReady:o},{default:f((()=>[P(a)?(u(),c(g,{key:0},[p(P(R),{url:"https://{s}.tile.osm.org/{z}/{x}/{y}.png"}),p(te,{options:{showCoverageOnHover:!1,chunkedLoading:!0}},{default:f((()=>[p(P(j),{"lat-lng":[47.7515953048815,8.757179159967961]},null,8,["lat-lng"]),p(P(j),{"lat-lng":[54.379448751829784,8.890621239746661]},null,8,["lat-lng"]),p(P(j),{"lat-lng":[48.41432462648719,11.172363685423019]},null,8,["lat-lng"]),p(P(j),{"lat-lng":[54.34757868763789,11.410597389004957]},null,8,["lat-lng"]),p(P(j),{"lat-lng":[51.741295879474464,13.693138753473695]},null,8,["lat-lng"]),p(P(j),{"lat-lng":[53.574845165295145,6.875185458821902]},null,8,["lat-lng"]),p(P(j),{"lat-lng":[51.42494690949777,6.901031944520698]},null,8,["lat-lng"])])),_:1})],64)):A("",!0)])),_:1},8,["modelValue","center"])])]))}});oe.__scopeId="data-v-10e222b0";C("data-v-687cce97");const se={class:"leaflet-map"},ne=h("Alarms"),ie=h(" | "),de=h("Nodes");L();var ue=w({setup(e){const t=k(),{queryParameters:a,updateQueryParameters:r,sort:l}=((e,t,a)=>{const r=k(),l=v(e),o=v(n({queryParameters:l.value},a));return{queryParameters:l,sort:e=>{const s=i(n({},l.value),{orderBy:e.sortField,order:1===e.sortOrder?"asc":"desc"});l.value=s,r.dispatch(t,a?i(n({},o.value),{queryParameters:s}):s)},updateQueryParameters:e=>l.value=e,payload:o}})({limit:5e3,offset:0},"nodesModule/getNodes");return t.dispatch("mapModule/getNodes",a.value),t.dispatch("mapModule/getAlarms",a.value),(e,t)=>{const a=d("router-link"),r=d("router-view");return u(),V(P(F.exports.Splitpanes),{class:"default-theme",horizontal:"",style:{height:"1000px"}},{default:f((()=>[p(P(F.exports.Pane),{"min-size":"1","max-size":"60"},{default:f((()=>[m("div",se,[p(oe)])])),_:1}),p(P(F.exports.Pane),{id:"map-pane-under"},{default:f((()=>[p(a,{to:{name:"MapAlarms"}},{default:f((()=>[ne])),_:1}),ie,p(a,{to:{name:"MapNodes"}},{default:f((()=>[de])),_:1}),p(r)])),_:1})])),_:1})}}});ue.__scopeId="data-v-687cce97";const ce={class:"mapnodes"},me={class:"map-nodes-grid"};var pe=w({setup(e){const t=v({}),a=v({floatingFilter:!0,resizable:!0,enableBrowserTooltips:!0,filter:"agTextColumnFilter"}),r=v([{headerName:"ID",field:"id",sortable:!0,headerTooltip:"ID",filter:"agNumberColumnFilter",comparator:(e,t)=>e-t},{headerName:"FOREIGN SOURCE",field:"foreignSource",sortable:!0,headerTooltip:"Foreign Source"},{headerName:"FOREIGN ID",field:"foreignId",sortable:!0,headerTooltip:"Foreign ID"},{headerName:"LABLE",field:"lable",sortable:!0,headerTooltip:"Lable"},{headerName:"LABLE SOURCE",field:"lableSource",sortable:!0,headerTooltip:"Lable Source"},{headerName:"LAST CAPABILITIES SCAN",field:"lastCapabilitiesScan",sortable:!0,headerTooltip:"Last Capabilities Scan",filter:"agDateColumnFilter",cellRenderer:e=>e.value?new Date(e.value).toLocaleDateString():""},{headerName:"PRIMARY INTERFACE",field:"primaryInterface",sortable:!0,headerTooltip:"Primary Interface"},{headerName:"SYSOBJECTID",field:"sysObjectid",sortable:!0,headerTooltip:"Sys Object ID"},{headerName:"SYSNAME",field:"sysName",sortable:!0,headerTooltip:"Sys Name"},{headerName:"SYSDESCRIPTION",field:"sysDescription",sortable:!0,headerTooltip:"Sys Description"},{headerName:"SYSCONTACT",field:"sysContact",sortable:!0,headerTooltip:"Sys Contact"},{headerName:"SYSLOCATION",field:"sysLocation",sortable:!0,headerTooltip:"Sys Location"}]),l=v([{id:1,foreignSource:"fs1"},{id:2,foreignSource:"fs2"}]);return(e,o)=>(u(),c("div",ce,[m("div",me,[p(P(x.AgGridVue),{style:{width:"100%",height:"600px"},class:"ag-theme-alpine",rowSelection:"multiple",columnDefs:r.value,rowData:l.value,defaultColDef:a.value,gridOptions:t.value,pagination:!0},null,8,["columnDefs","rowData","defaultColDef","gridOptions"])])]))}});const fe={},ge={class:"mapalarm"},he=[m("h4",null,"This is the alarm page",-1)];fe.render=function(e,t){return u(),c("div",ge,he)};const ve=[{path:"/",name:"Home",component:Q},{path:"/map",name:"Map",component:ue,children:[{path:"",name:"MapNodes",component:pe},{path:"alarms",name:"MapAlarms",component:fe}]}],ye=z({history:H("/"),routes:ve}),Se=$.create({baseURL:"/opennms/api/v2".toString()||"/opennms/api/v2",withCredentials:!0}),Ee=(e,t)=>{let a=t+"?",r="";for(const l in e)r=`${r}${l}=${e[l]}&`;return a+=r,a.slice(0,-1)};var Ne=async e=>{let t="";e&&(t=Ee(e,"/nodes"));try{const e=await Se.get(t||"/nodes");return 204===e.status?{node:[],totalCount:0,count:0,offset:0}:e.data}catch(a){return!1}},be=async e=>{let t="";e&&(t=Ee(e,"/alarms"));try{const e=await Se.get(t||"/alarms");return 204===e.status?{alarm:[],totalCount:0,count:0,offset:0}:e.data}catch(a){return!1}};var _e=q({modules:{mapModule:{state:{nodesWithCoordinates:[],alarms:[],interestedNodesID:[],edges:[]},mutations:{SAVE_NODES_TO_STATE:(e,t)=>{e.nodesWithCoordinates=[...t]},SAVE_ALARMS_TO_STATE:(e,t)=>{e.alarms=[...t]},SAVE_INTERESTED_NODES_ID:(e,t)=>{e.interestedNodesID=[...t]},SAVE_NODE_EDGES:(e,t)=>{e.edges=[...t]}},actions:{getNodes:async(e,t)=>{const a=await Ne(t);if(a){let t=a.node.filter((e=>!(null==e.assetRecord.latitude||0===e.assetRecord.latitude.length||null==e.assetRecord.longitude||0===e.assetRecord.longitude.length)));e.commit("SAVE_NODES_TO_STATE",t),e.commit("SAVE_INTERESTED_NODES_ID",t.map((e=>e.id)))}},getAlarms:async(e,t)=>{const a=await be(t);e.commit("SAVE_ALARMS_TO_STATE",a.alarm)},resetInterestedNodesID:(e,t)=>{e.commit("SET_INTERESTED_NODES_ID",t.nodesWithCoordinates.map((e=>e.id)))},setInterestedNodesId:(e,t)=>{e.commit("SAVE_INTERESTED_NODES_ID",t)}},getters:{getInterestedNodesID:e=>e.interestedNodesID,getInterestedNodes:e=>e.nodesWithCoordinates.filter((t=>e.interestedNodesID.includes(t.id))),getAlarmsFromSelectedNodes:(e,t)=>{let a=t.getInterestedNodes.map((e=>e.label));return e.alarms.filter((e=>a.includes(e.nodeLabel)))},getEdges:e=>e.edges},namespaced:!0}}});B(G).use(_e).use(ye).mount("#app");
