import { createStore } from "vuex";
import AlarmsService from "@/services/AlarmsService.js";
import NodesService from "@/services/NodesService.js";
import GraphService from "@/services/GraphService.js";

export default createStore({
  state: {
    nodes: [],
    alarms: [],
    monitoredNodesID: [],
    edges: []
  },
  mutations: {
    SET_NODES(state, nodes) {
      state.nodes = nodes
    },
    SET_ALARMS(state, alarms) {
      state.alarms = alarms
    },
    SET_SELECTED_NODES_ID(state, ids) {
      state.monitoredNodesID = ids
    },
    SET_NODE_EDGES(state, edges) {
      state.edges = edges
    }
  },
  actions: {
    fetchNodes({ commit }) {
      return NodesService.getNodes()
        .then(response => {
          commit("SET_NODES", response.data.node),
            commit("SET_SELECTED_NODES_ID", response.data.node.map(node => node.id))
        })
        .catch(error => {
          throw (error)
        })
    },
    fetchAlarms({ commit }) {
      return AlarmsService.getAlarms()
        .then(response => {
          commit("SET_ALARMS", response.data.alarm)
        })
        .catch(error => {
          throw (error)
        })
    },
    resetMonitoredNodesID({ commit }) {
      commit("SET_SELECTED_NODES_ID", this.state.nodes.map(node => node.id))
    },
    fetchNodesGraph({ commit }) {
      return GraphService.getNodesGraph()
        .then(response => {
          let edges = []
          response.data.edges.forEach((e) => {
            let edge = []
            edge.push(e.source.id)
            edge.push(e.target.id)
            edges.push(edge)
          });
          commit("SET_NODE_EDGES", edges)
        })
        .catch(error => {
          throw (error)
        })
    }
  },
  getters: {
    getMonitoredNodes: state => {
      return state.nodes.filter(node => state.monitoredNodesID.includes(node.id));
    },
  },
  modules: {},
});
