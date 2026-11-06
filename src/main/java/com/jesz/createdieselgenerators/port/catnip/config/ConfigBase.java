package com.jesz.createdieselgenerators.port.catnip.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Catnip's {@code ConfigBase} on NeoForge's config spec (Create 6, MIT).
 *
 * <p>PORT-NOTE(fabric): Create Fly replaced catnip's config base with its own JSON config files. Railways keeps its TOML
 * configs through Forge Config API Port, which provides {@link ModConfigSpec} on Fabric, so the original base class
 * lives here.</p>
 */
public abstract class ConfigBase {

	public ModConfigSpec specification;

	protected int depth;
	protected List<CValue<?, ?>> allValues;
	protected List<ConfigBase> children;

	public void registerAll(final ModConfigSpec.Builder builder) {
		for (CValue<?, ?> cValue : this.allValues)
			cValue.register(builder);
	}

	public void onLoad() {
		if (this.children != null)
			this.children.forEach(ConfigBase::onLoad);
	}

	public void onReload() {
		if (this.children != null)
			this.children.forEach(ConfigBase::onReload);
	}

	public abstract String getName();

	@FunctionalInterface
	protected interface IValueProvider<V, T extends ConfigValue<V>> extends Function<ModConfigSpec.Builder, T> {
	}

	protected ConfigBool b(boolean current, String name, String... comment) {
		return new ConfigBool(name, current, comment);
	}

	protected ConfigFloat f(float current, float min, float max, String name, String... comment) {
		return new ConfigFloat(name, current, min, max, comment);
	}

	protected ConfigFloat f(float current, float min, String name, String... comment) {
		return this.f(current, min, Float.MAX_VALUE, name, comment);
	}

	protected ConfigInt i(int current, int min, int max, String name, String... comment) {
		return new ConfigInt(name, current, min, max, comment);
	}

	protected ConfigInt i(int current, int min, String name, String... comment) {
		return this.i(current, min, Integer.MAX_VALUE, name, comment);
	}

	protected ConfigInt i(int current, String name, String... comment) {
		return this.i(current, Integer.MIN_VALUE, Integer.MAX_VALUE, name, comment);
	}

	protected <T extends Enum<T>> ConfigEnum<T> e(T defaultValue, String name, String... comment) {
		return new ConfigEnum<>(name, defaultValue, comment);
	}

	protected ConfigGroup group(int depth, String name, String... comment) {
		return new ConfigGroup(name, depth, comment);
	}

	protected <T extends ConfigBase> T nested(int depth, Supplier<T> constructor, String... comment) {
		T config = constructor.get();
		new ConfigGroup(config.getName(), depth, comment);
		new CValue<Boolean, BooleanValue>(config.getName(), builder -> {
			config.depth = depth;
			config.registerAll(builder);
			if (config.depth > depth)
				builder.pop(config.depth - depth);
			return null;
		});
		if (this.children == null)
			this.children = new ArrayList<>();
		this.children.add(config);
		return config;
	}

	public class CValue<V, T extends ConfigValue<V>> {

		protected ConfigValue<V> value;
		protected String name;
		private final IValueProvider<V, T> provider;

		public CValue(String name, IValueProvider<V, T> provider, String... comment) {
			this.name = name;
			this.provider = builder -> {
				this.addComments(builder, comment);
				return provider.apply(builder);
			};
			if (ConfigBase.this.allValues == null)
				ConfigBase.this.allValues = new ArrayList<>();
			ConfigBase.this.allValues.add(this);
		}

		public void addComments(ModConfigSpec.Builder builder, String... comment) {
			if (comment.length > 0) {
				String[] comments = new String[comment.length + 1];
				comments[0] = ".";
				System.arraycopy(comment, 0, comments, 1, comment.length);
				builder.comment(comments);
			} else {
				builder.comment(".");
			}
		}

		public void register(ModConfigSpec.Builder builder) {
			this.value = this.provider.apply(builder);
		}

		public V get() {
			return this.value.get();
		}

		public void set(V value) {
			this.value.set(value);
		}

		public String getName() {
			return this.name;
		}

	}

	/** Marker for config groups. */
	public class ConfigGroup extends CValue<Boolean, BooleanValue> {

		private final int groupDepth;
		private final String[] comment;

		public ConfigGroup(String name, int depth, String... comment) {
			super(name, builder -> null, comment);
			this.groupDepth = depth;
			this.comment = comment;
		}

		@Override
		public void register(ModConfigSpec.Builder builder) {
			if (ConfigBase.this.depth > this.groupDepth)
				builder.pop(ConfigBase.this.depth - this.groupDepth);
			ConfigBase.this.depth = this.groupDepth;
			this.addComments(builder, this.comment);
			builder.push(this.getName());
			ConfigBase.this.depth++;
		}

	}

	public class ConfigBool extends CValue<Boolean, BooleanValue> {

		public ConfigBool(String name, boolean def, String... comment) {
			super(name, builder -> builder.define(name, def), comment);
		}

	}

	public class ConfigEnum<T extends Enum<T>> extends CValue<T, EnumValue<T>> {

		public ConfigEnum(String name, T defaultValue, String[] comment) {
			super(name, builder -> builder.defineEnum(name, defaultValue), comment);
		}

	}

	public class ConfigFloat extends CValue<Double, DoubleValue> {

		public ConfigFloat(String name, float current, float min, float max, String... comment) {
			super(name, builder -> builder.defineInRange(name, current, min, max), comment);
		}

		public float getF() {
			return this.get().floatValue();
		}

	}

	public class ConfigInt extends CValue<Integer, IntValue> {

		public ConfigInt(String name, int current, int min, int max, String... comment) {
			super(name, builder -> builder.defineInRange(name, current, min, max), comment);
		}

	}

}
