import 'material-design-icons-iconfont/dist/material-design-icons.css' // Ensure you are using css-loader

import Vue from 'vue'
import Vuex from 'vuex'
Vue.use(Vuex)

import App from './App.vue'

import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css'
Vue.use(Vuetify, {
	iconfont: 'md'
})

Vue.config.productionTip = false

const app = new Vue({
	render: h => h(App),
});

app.$mount('#app');