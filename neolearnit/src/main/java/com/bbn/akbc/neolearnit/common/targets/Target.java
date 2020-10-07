package com.bbn.akbc.neolearnit.common.targets;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.constraints.MatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.SlotMatchConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EntityTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MustCoreferConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MustNotCoreferConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.SpanningTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.properties.TargetProperty;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.serif.patterns.Pattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.*;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Target {

    @JsonProperty
    private String name;
    @JsonProperty
    private final String description;
    @JsonProperty
    private final List<MatchConstraint> constraints;
    @JsonProperty
    private final Map<String, TargetProperty> properties;
    @JsonProperty
    private final List<TargetSlot> slots;
    @JsonProperty
    private final double patternConfidence;

    private boolean isAceType = false;


    @JsonCreator
    private Target(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("constraints") List<MatchConstraint> constraints,
            @JsonProperty("properties") Map<String, TargetProperty> properties,
            @JsonProperty("slots") List<TargetSlot> slots,
            @JsonProperty("patternConfidence") double patternConfidence) {
        this(name, description, constraints, properties, slots, patternConfidence, false);
    }

    private Target(String name,
                   String description,
                   List<MatchConstraint> constraints,
                   Map<String, TargetProperty> properties,
                   List<TargetSlot> slots,
                   double patternConfidence,
                   boolean isAceType) {
        this.name = name;
        this.description = description;
        this.constraints = new ArrayList<MatchConstraint>(constraints);
        this.properties = new HashMap<String, TargetProperty>(properties);
        this.slots = new ArrayList<TargetSlot>(slots);
        this.patternConfidence = patternConfidence;
        this.isAceType = isAceType;
    }

    public static class Builder {
        private final String name;
        private String description;
        private final ImmutableList.Builder<MatchConstraint> constraintBuilder;
        private final ImmutableMap.Builder<String, TargetProperty> propertiesBuilder;
        private final List<TargetSlot> slots;
        private double patternConfidence;

        private boolean isAceType;

        public Builder(String name) {
            this.name = name;
            this.description = "";
            this.constraintBuilder = new ImmutableList.Builder<MatchConstraint>();
            this.propertiesBuilder = new ImmutableMap.Builder<String, TargetProperty>();
            this.slots = new ArrayList<TargetSlot>();
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAddedConstraint(MatchConstraint constraint) {
            this.constraintBuilder.add(constraint);
            return this;
        }

        public Builder withAddedProperty(TargetProperty property) {
            this.propertiesBuilder.put(property.getName(), property);
            return this;
        }

        public Builder withTargetSlot(TargetSlot slot) {
            slots.add(slot);
            return this;
        }

        public Builder withPatternConfidenceCutoff(double cutoff) {
            this.patternConfidence = cutoff;
            return this;
        }

        public Builder setIsAceType(boolean isAceType) {
            this.isAceType = isAceType;
            return this;
        }


        public Target build() {
            Collections.sort(slots);
            return new Target(name, description, constraintBuilder.build(),
                    propertiesBuilder.build(), slots, patternConfidence);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {this.name = name;}

    public String getDescription() {
        return description;
    }

    public List<TargetSlot> getSlots() {
        return slots;
    }

    public TargetSlot getSlot(int i) {
        return slots.get(i);
    }

    public double getPatternConfidenceCutoff() {
        return patternConfidence;
    }

    public Pattern makeSlotBrandyPattern(String factType, int slot, Iterable<Restriction> restrictions) {
        return slots.get(slot).makeBrandyPattern(factType, this.name, this.isReflexive(), restrictions);
    }

    /**
     * Returns a string pointing to the mappings sub directory that
     * should be used to prepare the target mapping for a given experiment
     * <p>
     * Return values should be one of "all_mention_no_coref_pairs",
     * "all_mention_coref_pairs", "all_mention_value_pairs"
     */
    public String getMappingsSubDir() {
        // if one slot1 can contain a value mention return "all_mention_value_pairs"
        for (TargetSlot slot : slots) {
            if (!slot.isMention()) {
                return "all_mention_value_pairs";
            }
        }

        List<MatchConstraint> constraints = this.getConstraints();
        for (MatchConstraint c : constraints) {
            if (c instanceof MustCoreferConstraint)
                return "all_mention_coref_pairs";
            else if (c instanceof MustNotCoreferConstraint)
                return "all_mention_no_coref_pairs";
        }

        return "everything";
    }

    public boolean isSymmetric() {
        return this.properties.containsKey(TargetProperty.SYMMETRIC);
    }

    public boolean allowEmptySets() {
        return this.properties.containsKey(TargetProperty.EMPTY_SETS);
    }

    public boolean allowStopwordPatterns() {
        return this.properties.containsKey(TargetProperty.STOPWORD_PATS);
    }

    public boolean doLexicalExpansion() {
        return this.properties.containsKey(TargetProperty.LEX_EXPANSION);
    }

    public boolean useSimpleProps() {
        return this.properties.containsKey(TargetProperty.SIMPLE_PROPS);
    }

    public boolean isReflexive() {
        for (MatchConstraint c : constraints) {
            if (c instanceof MustCoreferConstraint) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBooleanProperty(String propertyName) {
        return this.properties.containsKey(propertyName);
    }

    public List<MatchConstraint> getConstraints() {
        return constraints;
    }

    public boolean validMatch(MatchInfo match, boolean evaluating) {
        for (MatchConstraint c : constraints) {
            if (evaluating && c.offForEvaluation()) continue;

            if (!c.valid(match)) {
                return false;
            }
        }

        for (TargetSlot slot : slots) {
            if (!slot.validMatch(match, evaluating)) {
                return false;
            }
        }
//		System.out.print(match.toString()+"Matched");
        return true;
    }

    public boolean validInstance(InstanceIdentifier id, Collection<Seed> seeds) {

        for (MatchConstraint c : constraints) {
            boolean isValid = c.valid(id, seeds, this);
            if (!isValid) {
//				System.out.println(c);
                return false;
            }
        }

        for (TargetSlot slot : slots) {
            boolean isValid = slot.validInstance(id, seeds);
            if (!isValid) {
//				System.out.println(slot);
                return false;
            }
        }

        return true;
    }

    public void serialize(String outfile) throws IOException {
        File parentDir = new File(new File(outfile).getParent());
        if (!parentDir.exists()) {
            parentDir.mkdir();
        }
        OutputStream out = new FileOutputStream(new File(outfile));
        StorageUtils.getDefaultMapper().writerWithDefaultPrettyPrinter().writeValue(out, this);
        out.close();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + constraints.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + properties.hashCode();
        result = prime * result + slots.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Target other = (Target) obj;
        if (!constraints.equals(other.constraints))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (patternConfidence != other.patternConfidence)
            return false;
        if (!properties.equals(other.properties))
            return false;
        return slots.equals(other.slots);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Target {\n");
        sb.append("  name=" + name + "\n");
        sb.append("  description=" + description + "\n");
        sb.append("  constraints=" + constraints + "\n");
        sb.append("  properties=" + properties + "\n");

        for (TargetSlot slot : slots) {
            sb.append("  slots:" + slot.toString() + "\n");
        }

        sb.append("  patternConfidence=" + patternConfidence + "\n");
        sb.append("}");

        return sb.toString();
    }

    public void writeToXmlFile(File fileXml) {
        String slot0validTypes = null, slot1validTypes = null;
        for (SlotMatchConstraint constraint : this.getSlot(0).getSlotConstraints()) {
            if (constraint instanceof EntityTypeConstraint) {
                slot0validTypes = ((EntityTypeConstraint) constraint).getValidTypesSpaceDelimited();
            }
        }
        for (SlotMatchConstraint constraint : this.getSlot(1).getSlotConstraints()) {
            if (constraint instanceof EntityTypeConstraint) {
                slot1validTypes = ((EntityTypeConstraint) constraint).getValidTypesSpaceDelimited();
            }
        }

        try {
            PrintWriter pwFileXml = new PrintWriter(new FileWriter(fileXml));
            pwFileXml.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pwFileXml.println("<targets>");
            pwFileXml.println("  <target name=\"" + this.name + "\" elf_ontology_type=\"" + this.name + "\"");
            pwFileXml.println("          description=\"" + this.description + "\">");
            pwFileXml.println("    <slot slotnum=\"0\" description=\"?\" type=\"mention\" elf_ontology_type=\"0_" + slot0validTypes.replace(" ", "_") + "\" elf_role=\"0_" + slot0validTypes.replace(" ", "_") + "\"");
            pwFileXml.println("            mention_constraints=\"(acetype " + slot0validTypes + ") (min-entitylevel DESC)\" />");
            pwFileXml.println("     <slot slotnum=\"1\" description=\"?\" type=\"mention\" elf_ontology_type=\"1_" + slot1validTypes.replace(" ", "_") + "\" elf_role=\"1_" + slot1validTypes.replace(" ", "_") + "\"");
            pwFileXml.println("            mention_constraints=\"(acetype " + slot1validTypes + ") (min-entitylevel DESC)\" />");
            pwFileXml.println("     <slot_pair a=\"1\" b=\"0\" symmetric=\"1\" must_not_corefer=\"1\"/>");
            pwFileXml.println("   </target>");
            pwFileXml.println("</targets>");
            pwFileXml.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isUnaryTarget() {
        for (MatchConstraint matchConstraint : this.constraints) {
            if (matchConstraint instanceof SpanningTypeConstraint) {
                SpanningTypeConstraint typeConstraint = (SpanningTypeConstraint) matchConstraint;
                if (typeConstraint.containsEmptySpanning()) {
                    return true;
                }
            }
        }
        return false;
    }

}
