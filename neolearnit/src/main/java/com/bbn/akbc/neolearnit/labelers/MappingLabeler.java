package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.mappings.groups.Mappings;

public interface MappingLabeler {
    // Maybe you want to merge mappings before labeling.
    Mappings LabelMappings(Mappings original) throws Exception;
}
