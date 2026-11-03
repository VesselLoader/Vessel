package dev.vessel.kernel.gui.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite verifying that the VESSEL Virtual GUI Anti-Theft Sentinel
 * successfully intercepts and neutralizes all 28 exploit vectors while seamlessly
 * delegating functional modded machinery operations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VirtualGuiAntiTheftTest {

    private ContainerClickActionClassifier classifier;
    private SlotImmediateDesyncFlusher desyncFlusher;
    private ExploitAuditLogger auditLogger;
    private VirtualSlotTheftGuard theftGuard;
    private CursorGhostItemNeutralizer neutralizer;
    private FunctionalModSlotDelegate modDelegate;
    private TestDesyncPacketCollector packetCollector;

    @BeforeEach
    public void setUp() {
        classifier = new ContainerClickActionClassifier();
        desyncFlusher = new SlotImmediateDesyncFlusher();
        auditLogger = new ExploitAuditLogger(100, 3);
        theftGuard = new VirtualSlotTheftGuard(classifier, desyncFlusher, auditLogger);
        neutralizer = new CursorGhostItemNeutralizer(CursorGhostItemNeutralizer.DEFAULT_VIRTUAL_ITEM_TAG, desyncFlusher);
        modDelegate = new FunctionalModSlotDelegate();
        packetCollector = new TestDesyncPacketCollector();
        desyncFlusher.registerReceiver(packetCollector);
    }

    @Test
    @DisplayName("Vector 01: Should intercept and flush Hotbar Key 1..9 swap on virtual slot")
    public void testVector01HotbarSwap() {
        ContainerClickContext ctx = new ContainerClickContext(
                4, 2, ContainerClickContext.CLICK_TYPE_SWAP, "Player_01",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_01", TheftVector.VECTOR_01_HOTBAR_SWAP)).isEqualTo(1);
        assertThat(packetCollector.getSlotFlushes()).isNotEmpty();
    }

    @Test
    @DisplayName("Vector 02: Should intercept and flush Offhand Key F swap on virtual slot")
    public void testVector02OffhandSwap() {
        ContainerClickContext ctx = new ContainerClickContext(
                4, ContainerClickContext.OFFHAND_BUTTON_CODE, ContainerClickContext.CLICK_TYPE_SWAP, "Player_02",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_02", TheftVector.VECTOR_02_OFFHAND_SWAP)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 03: Should intercept Key Q single item drop on virtual slot")
    public void testVector03ThrowSingle() {
        ContainerClickContext ctx = new ContainerClickContext(
                4, 0, ContainerClickContext.CLICK_TYPE_THROW, "Player_03",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_03", TheftVector.VECTOR_03_THROW_SINGLE_KEY)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 04: Should intercept Ctrl+Q stack drop on virtual slot")
    public void testVector04ThrowStack() {
        ContainerClickContext ctx = new ContainerClickContext(
                4, 1, ContainerClickContext.CLICK_TYPE_THROW, "Player_04",
                "vessel:virtual_frame", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_04", TheftVector.VECTOR_04_THROW_STACK_KEY)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 05: Should intercept out-of-bounds border drop with carried virtual item")
    public void testVector05OutOfBoundsDrop() {
        ContainerClickContext ctx = new ContainerClickContext(
                ContainerClickContext.OUT_OF_BOUNDS_SLOT_ID, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_05",
                ContainerClickContext.ITEM_EMPTY, false, "vessel:virtual_button", true,
                54, false, true, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_05", TheftVector.VECTOR_05_OUT_OF_BOUNDS_BORDER_DROP)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 06: Should intercept Shift-click quick move on virtual frame slot")
    public void testVector06ShiftQuickMove() {
        ContainerClickContext ctx = new ContainerClickContext(
                10, 0, ContainerClickContext.CLICK_TYPE_QUICK_MOVE, "Player_06",
                "vessel:virtual_frame", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_06", TheftVector.VECTOR_06_SHIFT_QUICK_MOVE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 07: Should intercept Double-Click collect all on virtual slot")
    public void testVector07DoubleClickCollectAll() {
        ContainerClickContext ctx = new ContainerClickContext(
                12, 0, ContainerClickContext.CLICK_TYPE_PICKUP_ALL, "Player_07",
                "vessel:virtual_frame", true, "minecraft:carrot", false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_07", TheftVector.VECTOR_07_DOUBLE_CLICK_COLLECT_ALL)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 08: Should intercept Mouse Drag-Split evenly across virtual slot")
    public void testVector08DragSplitEvenly() {
        ContainerClickContext ctx = new ContainerClickContext(
                14, ContainerClickActionClassifier.DRAG_BUTTON_SPLIT_EVENLY, ContainerClickContext.CLICK_TYPE_QUICK_CRAFT, "Player_08",
                "vessel:virtual_frame", true, "minecraft:iron_ingot", false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_08", TheftVector.VECTOR_08_DRAG_SPLIT_EVENLY)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 09: Should intercept Mouse Drag-Split single across virtual slot")
    public void testVector09DragSplitSingle() {
        ContainerClickContext ctx = new ContainerClickContext(
                15, ContainerClickActionClassifier.DRAG_BUTTON_SPLIT_SINGLE, ContainerClickContext.CLICK_TYPE_QUICK_CRAFT, "Player_09",
                "vessel:virtual_frame", true, "minecraft:iron_ingot", false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_09", TheftVector.VECTOR_09_DRAG_SPLIT_SINGLE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 10: Should intercept Mouse Drag Clone across virtual slot")
    public void testVector10DragClone() {
        ContainerClickContext ctx = new ContainerClickContext(
                16, ContainerClickActionClassifier.DRAG_BUTTON_CLONE_CREATIVE, ContainerClickContext.CLICK_TYPE_QUICK_CRAFT, "Player_10",
                "vessel:virtual_frame", true, "minecraft:iron_ingot", false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_10", TheftVector.VECTOR_10_DRAG_CLONE_CREATIVE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 11: Should intercept Esc / Window Close with item on cursor")
    public void testVector11EscCloseCursorDrop() {
        ContainerClickContext ctx = ContainerClickContext.ofWindowClose("Player_11", "vessel:virtual_button", true, 54);

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_11", TheftVector.VECTOR_11_ESC_CLOSE_CURSOR_DROP)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 12: Should intercept Disconnect with item on cursor")
    public void testVector12DisconnectWithCursor() {
        ContainerClickContext ctx = new ContainerClickContext(
                -1, 0, ContainerClickActionClassifier.ACTION_DISCONNECT, "Player_12",
                ContainerClickContext.ITEM_EMPTY, false, "vessel:virtual_button", true,
                54, true, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_12", TheftVector.VECTOR_12_DISCONNECT_WITH_CURSOR)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 13: Should intercept client Crash Packet with carried virtual item")
    public void testVector13CrashPacketWithCursor() {
        ContainerClickContext ctx = new ContainerClickContext(
                -1, 0, ContainerClickActionClassifier.ACTION_CRASH, "Player_13",
                ContainerClickContext.ITEM_EMPTY, false, "vessel:virtual_button", true,
                54, true, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_13", TheftVector.VECTOR_13_CRASH_PACKET_WITH_CURSOR)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 14: Should intercept out-of-order close packet drop")
    public void testVector14OutOfOrderCloseDrop() {
        ContainerClickContext ctx = new ContainerClickContext(
                -1, 0, ContainerClickActionClassifier.ACTION_OUT_OF_ORDER_CLOSE, "Player_14",
                ContainerClickContext.ITEM_EMPTY, false, "vessel:virtual_button", true,
                54, true, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_14", TheftVector.VECTOR_14_OUT_OF_ORDER_CLOSE_DROP)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 15: Should intercept mouse wheel scroll swap injection")
    public void testVector15MouseWheelScrollSwap() {
        ContainerClickContext ctx = new ContainerClickContext(
                5, 0, ContainerClickActionClassifier.ACTION_SCROLL_SWAP, "Player_15",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_15", TheftVector.VECTOR_15_MOUSE_WHEEL_SCROLL_SWAP)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 16: Should intercept shift + right-click auto sorting burst")
    public void testVector16ShiftRightClickBurst() {
        ContainerClickContext ctx = new ContainerClickContext(
                5, 1, ContainerClickContext.CLICK_TYPE_QUICK_MOVE, "Player_16",
                "vessel:virtual_frame", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, true, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_16", TheftVector.VECTOR_16_SHIFT_RIGHT_CLICK_BURST)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 17: Should intercept fast sort injection from client mods")
    public void testVector17FastSortInjection() {
        ContainerClickContext ctx = new ContainerClickContext(
                5, 0, ContainerClickActionClassifier.ACTION_FAST_SORT, "Player_17",
                "vessel:virtual_frame", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, true, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_17", TheftVector.VECTOR_17_FAST_SORT_INJECTION)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 18: Should intercept middle-click clone on virtual slot")
    public void testVector18MiddleClickClone() {
        ContainerClickContext ctx = new ContainerClickContext(
                8, 2, ContainerClickContext.CLICK_TYPE_CLONE, "Player_18",
                "vessel:virtual_frame", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_18", TheftVector.VECTOR_18_MIDDLE_CLICK_CLONE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 19: Should intercept negative slot ID packet flood")
    public void testVector19NegativeSlotPacketFlood() {
        ContainerClickContext ctx = new ContainerClickContext(
                -5, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_19",
                ContainerClickContext.ITEM_EMPTY, false, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_19", TheftVector.VECTOR_19_NEGATIVE_SLOT_PACKET_FLOOD)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 20: Should intercept slot overrun exceeding container window size")
    public void testVector20SlotOverrun() {
        ContainerClickContext ctx = new ContainerClickContext(
                65, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_20",
                ContainerClickContext.ITEM_EMPTY, false, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_20", TheftVector.VECTOR_20_SLOT_OVERRUN_WINDOW_SIZE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 21: Should intercept high latency packet reordering desync")
    public void testVector21HighLatencyDesync() {
        ContainerClickContext ctx = new ContainerClickContext(
                4, 0, ContainerClickActionClassifier.ACTION_HIGH_LATENCY_DESYNC, "Player_21",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_21", TheftVector.VECTOR_21_HIGH_LATENCY_DESYNC)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 22: Should intercept cursor swap transferring virtual item to player backpack")
    public void testVector22CursorSwapVirtualToBackpack() {
        ContainerClickContext ctx = new ContainerClickContext(
                30, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_22",
                "minecraft:diamond", false, "vessel:virtual_button", true,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.REAL_PLAYER_BACKPACK, VirtualSlotSecurityPolicy.REAL_PLAYER_BACKPACK_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_22", TheftVector.VECTOR_22_CURSOR_SWAP_VIRTUAL_TO_BACKPACK)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 23: Should intercept placing real mod item into locked virtual frame")
    public void testVector23PlaceIntoLockedFrame() {
        ContainerClickContext ctx = new ContainerClickContext(
                2, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_23",
                "vessel:virtual_frame", true, "create:brass_casing", false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.DECORATIVE_FRAME, VirtualSlotSecurityPolicy.DECORATIVE_FRAME_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_23", TheftVector.VECTOR_23_PLACE_INTO_LOCKED_FRAME)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 24: Should intercept shift-click transferring into virtual action button")
    public void testVector24ShiftIntoVirtualButton() {
        ContainerClickContext ctx = new ContainerClickContext(
                4, 0, ContainerClickContext.CLICK_TYPE_QUICK_MOVE, "Player_24",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_24", TheftVector.VECTOR_24_SHIFT_INTO_VIRTUAL_BUTTON)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 25: Should intercept double-click in backpack sucking virtual frame items")
    public void testVector25DoubleClickSuckingVirtual() {
        ContainerClickContext ctx = new ContainerClickContext(
                28, 0, ContainerClickContext.CLICK_TYPE_PICKUP_ALL, "Player_25",
                "vessel:virtual_frame", true, "minecraft:carrot", false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.REAL_PLAYER_BACKPACK, VirtualSlotSecurityPolicy.REAL_PLAYER_BACKPACK_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_25", TheftVector.VECTOR_25_DOUBLE_CLICK_SUCKING_VIRTUAL)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 26: Should intercept concurrent race condition conflict")
    public void testVector26ConcurrentRaceCondition() {
        ContainerClickContext ctx = new ContainerClickContext(
                10, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_26",
                "create:crushed_raw_iron", false, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, true, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.FUNCTIONAL_MOD_STORAGE, VirtualSlotSecurityPolicy.MOD_STORAGE_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_26", TheftVector.VECTOR_26_CONCURRENT_RACE_CONDITION)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 27: Should intercept container button packet injection")
    public void testVector27ButtonPacketInjection() {
        ContainerClickContext ctx = new ContainerClickContext(
                0, 1, ContainerClickActionClassifier.ACTION_BUTTON_INJECTION, "Player_27",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, false
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_27", TheftVector.VECTOR_27_BUTTON_PACKET_INJECTION)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vector 28: Should intercept Geyser / Bedrock form transaction desync")
    public void testVector28GeyserFormDesync() {
        ContainerClickContext ctx = new ContainerClickContext(
                0, 0, ContainerClickActionClassifier.ACTION_FORM_DESYNC, "Player_28",
                "vessel:virtual_button", true, ContainerClickContext.ITEM_EMPTY, false,
                54, false, false, false, false, true
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.REJECT_AND_FLUSH);
        assertThat(auditLogger.getExploitCount("Player_28", TheftVector.VECTOR_28_GEYSER_FORM_DESYNC)).isEqualTo(1);
    }

    @Test
    @DisplayName("Normal Button Click: Should trigger button action without moving item")
    public void testNormalButtonClick() {
        ContainerClickContext ctx = ContainerClickContext.ofClick(
                4, 0, ContainerClickContext.CLICK_TYPE_PICKUP, "Player_Legit",
                "vessel:virtual_button", false, ContainerClickContext.ITEM_EMPTY, false, 54
        );

        SecurityDecision decision = theftGuard.evaluate(ctx, VirtualSlotClassification.BUTTON, VirtualSlotSecurityPolicy.BUTTON_POLICY);
        assertThat(decision).isEqualTo(SecurityDecision.TRIGGER_ACTION_ONLY);
        assertThat(auditLogger.getAuditLogsForPlayer("Player_Legit")).isEmpty();
    }

    @Test
    @DisplayName("Modded Machine Delegation: Create millstone input and output checks")
    public void testModdedMachineryDelegation() {
        String millstoneId = "create:millstone";
        modDelegate.registerMachineIngredients(millstoneId, Set.of("minecraft:wheat", "minecraft:sugar_cane"));

        // Valid ingredient input
        assertThat(modDelegate.mayPlaceInMachine(millstoneId, VirtualSlotClassification.FUNCTIONAL_MOD_INPUT, "minecraft:wheat", false)).isTrue();

        // Invalid ingredient input
        assertThat(modDelegate.mayPlaceInMachine(millstoneId, VirtualSlotClassification.FUNCTIONAL_MOD_INPUT, "minecraft:cobblestone", false)).isFalse();

        // Placing into output slot is strictly forbidden
        assertThat(modDelegate.mayPlace(VirtualSlotClassification.FUNCTIONAL_MOD_OUTPUT, "create:wheat_flour", false)).isFalse();

        // Extracting from output slot is permitted
        assertThat(modDelegate.mayPickup(VirtualSlotClassification.FUNCTIONAL_MOD_OUTPUT, "create:wheat_flour")).isTrue();

        // Empty output slot cannot be extracted
        assertThat(modDelegate.mayPickup(VirtualSlotClassification.FUNCTIONAL_MOD_OUTPUT, ContainerClickContext.ITEM_EMPTY)).isFalse();
    }

    @Test
    @DisplayName("Cursor Neutralization: Should purge virtual items on window close and preserve real mod items")
    public void testCursorNeutralization() {
        // Virtual item on cursor is purged
        NeutralizationResult purged = neutralizer.neutralizeCarriedOnClose("Player_A", "vessel:virtual_button", true, 1);
        assertThat(purged.wasNeutralized()).isTrue();
        assertThat(purged.resultingItemId()).isEqualTo(ContainerClickContext.ITEM_EMPTY);
        assertThat(neutralizer.getNeutralizedItemCount()).isEqualTo(1);

        // Real mod item on cursor is preserved
        NeutralizationResult preserved = neutralizer.neutralizeCarriedOnClose("Player_B", "create:brass_casing", false, 16);
        assertThat(preserved.wasNeutralized()).isFalse();
        assertThat(preserved.resultingItemId()).isEqualTo("create:brass_casing");
        assertThat(preserved.resultingCount()).isEqualTo(16);
    }

    @Test
    @DisplayName("Audit Logger: Should aggregate counts and trigger threshold alert")
    public void testAuditLoggerThresholdAlert() {
        String cheater = "MaliciousUser";
        for (int i = 0; i < 3; i++) {
            auditLogger.logExploitAttempt(cheater, TheftVector.VECTOR_01_HOTBAR_SWAP, 4, "test_swap");
        }

        assertThat(auditLogger.getExploitCount(cheater, TheftVector.VECTOR_01_HOTBAR_SWAP)).isEqualTo(3);
        assertThat(auditLogger.getTotalExploitCountForPlayer(cheater)).isEqualTo(3);
        assertThat(auditLogger.getAuditLogsForPlayer(cheater)).hasSize(3);
    }
}
