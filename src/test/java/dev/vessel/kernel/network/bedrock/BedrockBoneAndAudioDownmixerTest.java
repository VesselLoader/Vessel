package dev.vessel.kernel.network.bedrock;

import dev.vessel.kernel.render.AudioDownmixProfile;
import dev.vessel.kernel.render.Spatial3dAudioDownmixer;
import dev.vessel.kernel.render.SpatialAudioDownmixResult;
import dev.vessel.kernel.render.SpatialAudioListenerPosition;
import dev.vessel.kernel.render.SpatialSoundSourcePosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for BedrockBoneHierarchyNormalizer and Spatial3dAudioDownmixer.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class BedrockBoneAndAudioDownmixerTest {

    @Test
    @DisplayName("Should normalize bone rotation order from Java Z-Y-X to Bedrock X-Y-Z")
    public void testBoneHierarchyNormalizer() {
        BedrockBoneHierarchyNormalizer normalizer = new BedrockBoneHierarchyNormalizer();

        // Register head bone with 0 rotation
        BedrockBoneHierarchyNode node0 = normalizer.registerBone(
                "head",
                null,
                0.0f, 24.0f, 0.0f,
                new EulerRotation3f(0.0f, 0.0f, 0.0f)
        );
        assertThat(node0.normalizedBedrockRotation().pitchDegrees()).isEqualTo(0.0f);
        assertThat(node0.normalizedBedrockRotation().yawDegrees()).isEqualTo(0.0f);
        assertThat(node0.normalizedBedrockRotation().rollDegrees()).isEqualTo(0.0f);

        // Register arm with 45 deg pitch
        BedrockBoneHierarchyNode armNode = normalizer.registerBone(
                "right_arm",
                "body",
                -5.0f, 22.0f, 0.0f,
                new EulerRotation3f(45.0f, 0.0f, 0.0f)
        );
        assertThat(armNode.normalizedBedrockRotation().pitchDegrees()).isCloseTo(45.0f, within(0.01f));

        // Animation tick batch update
        var updated = normalizer.normalizeAnimationTick(Map.of(
                "head", new EulerRotation3f(15.0f, 30.0f, 0.0f)
        ));
        assertThat(updated).hasSize(1);
        assertThat(normalizer.getBone("head")).isNotNull();
    }

    @Test
    @DisplayName("Should downmix 5.1 multichannel audio to mono according to ITU-R BS.775")
    public void testSpatialAudioDownmixer() {
        Spatial3dAudioDownmixer downmixer = new Spatial3dAudioDownmixer(AudioDownmixProfile.ITU_R_BS_775);

        int sampleCount = 4;
        float[][] surround51 = new float[][] {
                new float[] { 0.5f, 0.5f, 0.5f, 0.5f }, // Front Left
                new float[] { 0.5f, 0.5f, 0.5f, 0.5f }, // Front Right
                new float[] { 0.2f, 0.2f, 0.2f, 0.2f }, // Center
                new float[] { 0.8f, 0.8f, 0.8f, 0.8f }, // LFE (gain = 0.0 in ITU-R BS.775)
                new float[] { 0.1f, 0.1f, 0.1f, 0.1f }, // Surround Left
                new float[] { 0.1f, 0.1f, 0.1f, 0.1f }  // Surround Right
        };

        float[] mono = downmixer.downmix51ToMono(surround51, sampleCount);
        assertThat(mono).hasSize(sampleCount);

        // Expected sample value:
        // (0.5 * 0.7071) + (0.5 * 0.7071) + (0.2 * 1.0) + (0.8 * 0.0) + (0.1 * 0.5) + (0.1 * 0.5)
        // = 0.7071 + 0.2 + 0.05 + 0.05 = 1.0071 -> normalized to <= 1.0
        assertThat(mono[0]).isLessThanOrEqualTo(1.0f);
        assertThat(mono[0]).isGreaterThan(0.9f);

        // Spatial 3D downmix test
        SpatialAudioListenerPosition listener = new SpatialAudioListenerPosition(0.0, 0.0, 0.0, 0.0, 0.0);
        SpatialSoundSourcePosition source = new SpatialSoundSourcePosition(10.0, 0.0, 0.0, 50.0, 1.0);

        SpatialAudioDownmixResult spatialResult = downmixer.downmixSpatialAudio(surround51, sampleCount, listener, source);
        assertThat(spatialResult.distanceMeters()).isCloseTo(10.0, within(0.01));
        assertThat(spatialResult.volumeGain()).isBetween(0.0, 1.0);
        assertThat(spatialResult.monoSamples()).hasSize(sampleCount);
    }
}
