package cr0s.warpdrive.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

public class WarpDriveText extends StringTextComponent {
	
	public WarpDriveText() {
		super("");
	}
	
	public WarpDriveText(@Nullable final Style style, final long value) {
		super("");
		
		final ITextComponent textComponent = new StringTextComponent(String.format("%d", value));
		if (style != null) {
			textComponent.setStyle(style);
		}
		append(textComponent);
	}
	
	public WarpDriveText(@Nullable final Style style, @Nonnull final String translationKey, final Object... args) {
		super("");
		
		final ITextComponent textComponent = new TranslationTextComponent(translationKey, args);
		final String textRaw = textComponent.getUnformattedText();
		if (textRaw.startsWith("[") && textRaw.endsWith("]")) {
			final ITextComponent textComponentNested = ITextComponent.Serializer.jsonToComponent(textRaw);
			if (textComponentNested != null) {
				append(textComponentNested);
			} else {
				append(textComponent);
			}
		} else {
			if (style != null) {
				textComponent.setStyle(style);
			}
			append(textComponent);
		}
	}
	
	@Nonnull
	@Override
	public String getUnformattedComponentText() {
		return "";
	}
	
	@Nonnull
	@Override
	public StringTextComponent deepCopy() {
		final WarpDriveText warpDriveText = new WarpDriveText();
		warpDriveText.setStyle(getStyle().createShallowCopy());
		
		for (final ITextComponent textComponent : getSiblings()) {
			warpDriveText.appendSibling(textComponent.deepCopy());
		}
		
		return warpDriveText;
	}
	
	public boolean isEmpty() {
		return siblings.isEmpty();
	}
	
	public WarpDriveText appendLineBreak() {
		if (siblings.isEmpty()) {
			return this;
		}
		appendSibling(new StringTextComponent("\n"));
		return this;
	}
	
	public WarpDriveText append(@Nonnull final ITextComponent textComponent) {
		if ( !textComponent.getUnformattedComponentText().isEmpty()
		  || !textComponent.getSiblings().isEmpty() ) {
			appendLineBreak();
			appendSibling(textComponent);
		}
		return this;
	}
	
	public WarpDriveText append(@Nullable final Style style, @Nonnull final String translationKey, final Object... args) {
		final ITextComponent textComponent = new TranslationTextComponent(translationKey, args);
		if (style != null) {
			textComponent.setStyle(style);
		}
		return append(textComponent);
	}
	
	public WarpDriveText appendInLine(@Nullable final Style style, @Nonnull final String translationKey, final Object... args) {
		final ITextComponent textComponent = new TranslationTextComponent(translationKey, args);
		if (style != null) {
			textComponent.setStyle(style);
		}
		appendSibling(textComponent);
		return this;
	}
	
	public WarpDriveText appendInLine(@Nullable final Style style, final long value) {
		final ITextComponent textComponent = new StringTextComponent(String.format("%d", value));
		if (style != null) {
			textComponent.setStyle(style);
		}
		appendSibling(textComponent);
		return this;
	}
	
	public WarpDriveText appendSibling(final WarpDriveText textComponent) {
		return (WarpDriveText) super.appendSibling(textComponent);
	}
}
