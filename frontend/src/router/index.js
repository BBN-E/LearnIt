import Vue from 'vue'
import Router from 'vue-router'
Vue.use(Router)
import LearnItMain from '@/components/LearnItMain/LearnItMain'
import LabeledMappingsView from '@/components/LabeledMappingsView'
import TimelineMain from '@/components/EventTimeline/TimelineMain'

export default new Router({
	routes: [
		{
			path: '/',
			name: 'LearnItMain',
			component: LearnItMain
		},
		{
			path: '/LabeledMappingsView',
			name: 'LabeledMappingsView',
			component: LabeledMappingsView
		},
		{
			path: '/TimelineMain',
			name: 'TimelineMain',
			component: TimelineMain
		}
	]
})