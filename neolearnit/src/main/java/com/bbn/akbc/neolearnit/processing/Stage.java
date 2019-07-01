package com.bbn.akbc.neolearnit.processing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;

public interface Stage<T extends PartialInformation> {

	public T processMappings(Mappings mappings);

	public T reduceInformation(Collection<T> inputs);

	public T processMappingFiles(Iterable<File> files);

	public T processPartialInfoFiles(Iterable<File> files);

	public void runOnFile(File input) throws IOException;

	public void runOnMappings(Mappings mappings);

	public void runStage(T input);

}
