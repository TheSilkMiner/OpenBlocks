package openblocks.client.model;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openmods.geometry.AabbBuilder;
import openmods.renderer.DisplayListWrapper;
import org.lwjgl.opengl.GL11;

public class ModelCartographer extends ModelBase {
	private static final float SCALE = 1.0f / 16.0f;
	private final ModelRenderer body;
	private final ModelRenderer base;

	private DisplayListWrapper eyeList;

	public ModelCartographer() {
		textureWidth = 32;
		textureHeight = 32;

		body = new ModelRenderer(this, 0, 0);
		body.addBox(-2.5f, -1.5f, -2.5f, 5, 2, 5);

		base = new ModelRenderer(this);
		body.addChild(base);

		base.setTextureOffset(0, 7);
		base.addBox(-1.5f, 0.5f, -1.5f, 3, 1, 3);

		base.setTextureOffset(0, 11);
		base.addBox(-0.5f, 0.5f, -2.5f, 1, 4, 1);

		base.setTextureOffset(4, 11);
		base.addBox(-0.5f, 0.5f, 1.5f, 1, 4, 1);

		MinecraftForge.EVENT_BUS.register(this);
	}

	public void renderEye(float baseRotation, float eyeRotation) {
		if (eyeList != null) {
			GL11.glPushMatrix();
			GL11.glTranslatef(0, 0.25f, 0);
			GL11.glRotated(Math.toDegrees(baseRotation) + 90, 0, 1, 0);
			GL11.glRotated(Math.toDegrees(eyeRotation), 1, 0, 0);
			GL11.glScalef(3 * SCALE, 3 * SCALE, 3 * SCALE);
			eyeList.compile();
			GL11.glPopMatrix();
		}
	}

	public void renderBase(float baseRotation) {
		base.rotateAngleY = baseRotation;
		body.render(SCALE);
	}

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent evt) {
		final ModelResourceLocation modelLocation = new ModelResourceLocation(Items.ENDER_EYE.getRegistryName(), "inventory");
		final IBakedModel bakedModel = evt.getModelRegistry().getObject(modelLocation);
		setEyeRender(bakedModel);
	}

	private void setEyeRender(final IBakedModel model) {
		final List<BakedQuad> quads = Lists.newArrayList();

		quads.addAll(model.getQuads(null, EnumFacing.WEST, 0));
		quads.addAll(model.getQuads(null, EnumFacing.EAST, 0));

		quads.addAll(model.getQuads(null, EnumFacing.NORTH, 0));
		quads.addAll(model.getQuads(null, EnumFacing.SOUTH, 0));

		quads.addAll(model.getQuads(null, EnumFacing.UP, 0));
		quads.addAll(model.getQuads(null, EnumFacing.DOWN, 0));

		quads.addAll(model.getQuads(null, null, 0));

		final AabbBuilder allBoundsBuilder = AabbBuilder.create();
		final AabbBuilder horizontalBoundsBuilder = AabbBuilder.create();

		for (BakedQuad quad : quads) {
			if (quad.getFace() == EnumFacing.EAST || quad.getFace() == EnumFacing.WEST)
				addQuad(quad, horizontalBoundsBuilder);
			addQuad(quad, allBoundsBuilder);
		}

		final AxisAlignedBB horizontalBounds = horizontalBoundsBuilder.build();
		final AxisAlignedBB allBounds = allBoundsBuilder.build();

		final AxisAlignedBB scaleBounds = !horizontalBounds.hasNaN()? horizontalBounds : allBounds;

		final double scale = 1.0 / (scaleBounds.maxX - scaleBounds.minX);

		final double middleX = (allBounds.maxX + allBounds.minX) / 2.0;
		final double middleY = (allBounds.maxY + allBounds.minY) / 2.0;
		final double middleZ = (allBounds.maxZ + allBounds.minZ) / 2.0;

		this.eyeList = new DisplayListWrapper() {
			@Override
			public void compile() {
				GL11.glPushMatrix();

				GL11.glScaled(scale, scale, scale);
				GL11.glTranslated(-middleX, -middleY, -middleZ);

				final Tessellator tessellator = Tessellator.getInstance();

				VertexBuffer vertexBuffer = tessellator.getBuffer();
				vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);

				for (BakedQuad quad : quads)
					vertexBuffer.addVertexData(quad.getVertexData());

				tessellator.draw();

				GL11.glPopMatrix();
			}
		};
	}

	private static void addQuad(BakedQuad quad, AabbBuilder aabbBuilder) {

		final int[] vertexData = quad.getVertexData();
		final ByteBuffer buffer = ByteBuffer.allocate(vertexData.length * Ints.BYTES);
		buffer.asIntBuffer().put(vertexData);

		final int vertexSize = quad.getFormat().getNextOffset();
		buffer.limit(vertexData.length * Ints.BYTES);

		for (int i = 0; i < 4; i++) {
			buffer.position(vertexSize * i);
			final float x = buffer.getFloat();
			final float y = buffer.getFloat();
			final float z = buffer.getFloat();

			aabbBuilder.addPoint(x, y, z);
		}
	}
}
