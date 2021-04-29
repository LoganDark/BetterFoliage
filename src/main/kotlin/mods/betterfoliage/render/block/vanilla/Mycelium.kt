package mods.betterfoliage.render.block.vanilla

import mods.betterfoliage.BetterFoliage
import mods.betterfoliage.render.ShadersModIntegration
import mods.betterfoliage.render.lighting.grassTuftLighting
import mods.betterfoliage.render.lighting.withLighting
import mods.betterfoliage.resource.discovery.BlockRenderKey
import mods.betterfoliage.resource.discovery.ModelDiscoveryBase
import mods.betterfoliage.resource.discovery.ModelDiscoveryContext
import mods.betterfoliage.model.Color
import mods.betterfoliage.model.SpriteSetDelegate
import mods.betterfoliage.model.WrappedBakedModel
import mods.betterfoliage.model.buildTufts
import mods.betterfoliage.model.meshifyStandard
import mods.betterfoliage.model.tuftModelSet
import mods.betterfoliage.model.tuftShapeSet
import mods.betterfoliage.util.Atlas
import mods.betterfoliage.util.LazyInvalidatable
import mods.betterfoliage.util.get
import mods.betterfoliage.util.offset
import mods.betterfoliage.util.plus
import mods.betterfoliage.util.randomI
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.client.render.model.BakedModel
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction.UP
import net.minecraft.world.BlockRenderView
import java.util.Random
import java.util.function.Consumer
import java.util.function.Supplier

object MyceliumKey : BlockRenderKey {
    override fun replace(model: BakedModel, state: BlockState) = MyceliumModel(meshifyStandard(model, state))
}

object MyceliumDiscovery : ModelDiscoveryBase() {
    override val logger = BetterFoliage.logDetail
    val myceliumBlocks = listOf(Blocks.MYCELIUM)
    override fun processModel(ctx: ModelDiscoveryContext, atlas: Consumer<Identifier>) =
        if (ctx.state.block in myceliumBlocks) MyceliumKey else null
}

class MyceliumModel(wrapped: BakedModel) : WrappedBakedModel(wrapped) {

    val tuftLighting = grassTuftLighting(UP)

    override fun emitBlockQuads(blockView: BlockRenderView, state: BlockState, pos: BlockPos, randomSupplier: Supplier<Random>, context: RenderContext) {
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context)

        val random = randomSupplier.get()
        if (BetterFoliage.config.enabled &&
            BetterFoliage.config.shortGrass.let { it.myceliumEnabled && random.nextInt(64) < it.population } &&
            blockView.getBlockState(pos + UP.offset).isAir
        ) {
            ShadersModIntegration.grass(context, BetterFoliage.config.shortGrass.shaderWind) {
                context.withLighting(tuftLighting) {
                    it.accept(myceliumTuftModels[random])
                }
            }
        }
    }

    companion object {
        val myceliumTuftSprites by SpriteSetDelegate(Atlas.BLOCKS) { idx ->
            Identifier(BetterFoliage.MOD_ID, "blocks/better_mycel_$idx")
        }
        val myceliumTuftModels by LazyInvalidatable(BetterFoliage.modelReplacer) {
            val shapes = BetterFoliage.config.shortGrass.let { tuftShapeSet(it.size, it.heightMin, it.heightMax, it.hOffset) }
            tuftModelSet(shapes, Color.white.asInt) { idx -> myceliumTuftSprites[randomI()] }.buildTufts()
        }
    }
}