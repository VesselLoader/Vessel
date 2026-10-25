package dev.vessel.kernel.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AotModelJsonToGeometryTranspiler and TextureAtlasMaxRectsBinPacker.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class AotGeometryAndTextureAtlasTest {

    @Test
    @DisplayName("Should transpile Java Block Model elements into Bedrock 1.21 Geometry JSON")
    public void testAotGeometryTranspiler() {
        AotModelJsonToGeometryTranspiler transpiler = new AotModelJsonToGeometryTranspiler(64, 64);

        JavaModelFace face = new JavaModelFace(new float[] { 0.0f, 0.0f, 16.0f, 16.0f }, "#all", 0, -1);
        JavaBlockModelElement element = new JavaBlockModelElement(
                new float[] { 0.0f, 0.0f, 0.0f },
                new float[] { 16.0f, 16.0f, 16.0f },
                Map.of("north", face, "south", face),
                new float[] { 8.0f, 8.0f, 8.0f },
                "y",
                45.0f
        );

        BedrockGeometryDefinition def = transpiler.transpileJavaElements(
                "geometry.vessel.test_block",
                List.of(element)
        );

        assertThat(def.geometryIdentifier()).isEqualTo("geometry.vessel.test_block");
        assertThat(def.textureWidth()).isEqualTo(64);
        assertThat(def.textureHeight()).isEqualTo(64);
        assertThat(def.bones()).hasSize(1);

        BedrockGeometryBone bone = def.bones().getFirst();
        assertThat(bone.cubes()).hasSize(1);
        BedrockGeometryCube cube = bone.cubes().getFirst();
        assertThat(cube.origin()).containsExactly(-8.0f, 0.0f, -8.0f);
        assertThat(cube.size()).containsExactly(16.0f, 16.0f, 16.0f);

        String json = transpiler.serializeToJson(def);
        assertThat(json).contains("\"format_version\": \"1.21.0\"");
        assertThat(json).contains("\"geometry.vessel.test_block\"");
        assertThat(json).contains("\"minecraft:geometry\"");
    }

    @Test
    @DisplayName("Should pack textures into optimal Power-of-Two atlas using MaxRects")
    public void testTextureAtlasBinPacker() {
        TextureAtlasMaxRectsBinPacker packer = new TextureAtlasMaxRectsBinPacker(256, 1024, 2);

        List<TextureAtlasInputNode> inputs = List.of(
                new TextureAtlasInputNode("texture_a", 64, 64),
                new TextureAtlasInputNode("texture_b", 32, 32),
                new TextureAtlasInputNode("texture_c", 128, 64),
                new TextureAtlasInputNode("texture_d", 16, 16)
        );

        TextureAtlasPackResult result = packer.pack(inputs);
        assertThat(result.isFullyPacked()).isTrue();
        assertThat(result.unplacedNodes()).isEmpty();
        assertThat(result.packedRegions()).hasSize(4);
        assertThat(TextureAtlasMaxRectsBinPacker.isPowerOfTwo(result.atlasWidth())).isTrue();
        assertThat(TextureAtlasMaxRectsBinPacker.isPowerOfTwo(result.atlasHeight())).isTrue();

        for (TextureAtlasPackedRegion region : result.packedRegions()) {
            assertThat(region.uMin()).isGreaterThanOrEqualTo(0.0f);
            assertThat(region.uMax()).isLessThanOrEqualTo(1.0f);
            assertThat(region.vMin()).isGreaterThanOrEqualTo(0.0f);
            assertThat(region.vMax()).isLessThanOrEqualTo(1.0f);
            assertThat(region.uMax()).isGreaterThan(region.uMin());
            assertThat(region.vMax()).isGreaterThan(region.vMin());
        }
    }
}
