package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.modelengine.bedrock.GeyserCustomGeometryTranslator;
import dev.vessel.kernel.modelengine.bedrock.GeyserEntityRegistrationBridge;
import dev.vessel.kernel.modelengine.bedrock.GeyserModelDefinition;
import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.BoneNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for GeyserMC Bedrock crossplay geometry translation and registration.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class GeyserBedrockCrossplayTest {

    @Test
    @DisplayName("GeyserCustomGeometryTranslator converts bone hierarchy to Bedrock geometry.json format")
    void testGeyserGeometryTranslation() {
        GeyserModelDefinition definition = new GeyserModelDefinition(
                "custom_dragon",
                "geometry.vessel.custom_dragon",
                "textures/entity/custom_dragon.png",
                128,
                128,
                "controller.render.dragon"
        );

        BoneHierarchyTree tree = new BoneHierarchyTree();
        BoneNode root = new BoneNode("root", null);
        root.setTranslation(0.0f, 1.0f, 0.0f);
        tree.registerBone(root);

        BoneNode head = new BoneNode("head", "root");
        head.setTranslation(0.0f, 0.5f, 1.0f);
        tree.registerBone(head);

        GeyserCustomGeometryTranslator translator = new GeyserCustomGeometryTranslator();
        String json = translator.translateToBedrockGeometry(definition, tree);

        assertThat(json).contains("\"format_version\": \"1.16.0\"");
        assertThat(json).contains("\"identifier\": \"geometry.vessel.custom_dragon\"");
        assertThat(json).contains("\"texture_width\": 128");
        assertThat(json).contains("\"texture_height\": 128");
        assertThat(json).contains("\"name\": \"root\"");
        assertThat(json).contains("\"name\": \"head\"");
        assertThat(json).contains("\"parent\": \"root\"");
        assertThat(json).contains("\"pivot\": [0.00, 16.00, 0.00]");
    }

    @Test
    @DisplayName("GeyserEntityRegistrationBridge registers custom entities and generates mappings JSON")
    void testGeyserEntityRegistrationBridge() {
        GeyserEntityRegistrationBridge bridge = new GeyserEntityRegistrationBridge();

        GeyserModelDefinition defGolem = new GeyserModelDefinition(
                "vessel_golem",
                "geometry.vessel.golem",
                "textures/entity/golem.png"
        );

        bridge.registerModel(defGolem);
        assertThat(bridge.getRegisteredModelCount()).isEqualTo(1);
        assertThat(bridge.getModel("vessel_golem")).isSameAs(defGolem);

        String mappingsJson = bridge.generateGeyserCustomEntityMappingsJson();
        assertThat(mappingsJson).contains("\"format_version\": \"1.0.0\"");
        assertThat(mappingsJson).contains("\"vessel_golem\": {");
        assertThat(mappingsJson).contains("\"bedrock_identifier\": \"geometry.vessel.golem\"");
        assertThat(mappingsJson).contains("\"texture\": \"textures/entity/golem.png\"");

        bridge.unregisterModel("vessel_golem");
        assertThat(bridge.getRegisteredModelCount()).isEqualTo(0);
    }
}
