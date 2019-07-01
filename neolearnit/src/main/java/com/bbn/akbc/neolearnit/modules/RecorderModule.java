package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.feature.AbstractRelationFeature;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class RecorderModule extends AbstractModule {

	@Override
	protected void configure() {

		/* -------------------------------------------
		 * CONFIGURE MAPPING STORAGE IMPLEMENATION
		 * -------------------------------------------*/


		// BY THE WAY THIS IS TERRIBLE AND A PLACE WHERE JAVA AND GUICE
		// ARE JUST WRONG BECAUSE GENERICS !!!!!!!!!!!!!!!!

		// what I wish I could do
		// bind(MapStorage.Builder.class).to(HashMapStorage.Builder.class);

		// we're going to use hash maps to back our lookups
		bind(new TypeLiteral<MapStorage.Builder<InstanceIdentifier,String>>() {})
			.to(new TypeLiteral<HashMapStorage.Builder<InstanceIdentifier,String>>() {});

		bind(new TypeLiteral<MapStorage.Builder<InstanceIdentifier,EvalAnswer>>() {})
			.to(new TypeLiteral<HashMapStorage.Builder<InstanceIdentifier,EvalAnswer>>() {});

		bind(new TypeLiteral<MapStorage.Builder<InstanceIdentifier,LearnitPattern>>() {})
			.to(new TypeLiteral<HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>>() {});

		bind(new TypeLiteral<MapStorage.Builder<InstanceIdentifier,Seed>>() {})
			.to(new TypeLiteral<HashMapStorage.Builder<InstanceIdentifier,Seed>>() {});

	/*
			bind(new TypeLiteral<MapStorage.Builder<InstanceIdentifier,CNNFeatureSet>>() {})
		.to(new TypeLiteral<HashMapStorage.Builder<InstanceIdentifier, CNNFeatureSet>>() {
		});
	*/

		bind(new TypeLiteral<MapStorage.Builder<InstanceIdentifier,AbstractRelationFeature>>() {})
		    .to(new TypeLiteral<HashMapStorage.Builder<InstanceIdentifier, AbstractRelationFeature>>() {
		    });


	}

}
