<template>
  <splitpanes class="default-theme" horizontal style="height: 1000px">
    <pane min-size="1" max-size="60">
      <div class="leaflet-map">
        <LeafletMap />
      </div>
    </pane>
    <pane id="map-pane-under">
      <router-link :to="{ name: 'MapNodes' }">NODES({{interestedNodesID.length}})</router-link>
      |
      <router-link :to="{ name: 'MapAlarms' }">ALARMS({{alarms.length}})</router-link>

      <router-view />
    </pane>
  </splitpanes>
</template>

<script setup lang="ts">
import LeafletMap from "../components/LeafletMap.vue";
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import { useStore } from "vuex";
import { computed, watch } from 'vue'

const store = useStore();

let interestedNodesID = computed(() => {
  return store.getters['mapModule/getInterestedNodesID'];
})

let alarms = computed(() => {
  return store.getters['mapModule/getAlarmsFromSelectedNodes'];
})

store.dispatch("mapModule/getNodes", {
  limit: 5000,
  offset: 0,
});

store.dispatch("mapModule/getAlarms", {
  limit: 5000,
  offset: 0,
});

store.dispatch("mapModule/getNodesGraphEdges");
</script>

<style scoped>
#map-pane-under {
  text-align: left;
}

#map-pane-under a {
  font-family: Arial;
  font-size: 14.5px;
  color: #2d515c;
}

#map-pane-under a.router-link-exact-active {
  font-size: 15.5px;
  color: #325c69;
  background-color: #cfd1df;
  font-weight: bold; 
  padding: 8px;
  border-radius: 3px;
}
</style>