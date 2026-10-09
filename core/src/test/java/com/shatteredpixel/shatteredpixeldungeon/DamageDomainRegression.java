package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DamageDomain;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicOnlyDomain;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalMagicNullDomain;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalOnlyDomain;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Crystal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.DisintegrationTrap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.GameSettings;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** 创建真实角色、装备和效果，验证实际扣血；替身只固定数值与隔离渲染。 */
public class DamageDomainRegression {

	private enum Domain {
		NONE("无领域", null),
		PHYSICAL("物理限定", PhysicalOnlyDomain.class),
		MAGIC("魔法限定", MagicOnlyDomain.class),
		NULL("物理与魔法失效", PhysicalMagicNullDomain.class);

		final String label;
		final Class<? extends DamageDomain> type;
		Domain(String label, Class<? extends DamageDomain> type) {
			this.label = label;
			this.type = type;
		}
		void apply(Char actor) {
			if (type != null) Buff.affect(actor, type);
		}
	}

	private enum Kind { PHYSICAL, MAGIC, OTHER, MIXED }
	private interface Scenario { int run(Fixture f, Domain domain) throws Exception; }
	private static int checks;
	private static final long SEED = 20261009L;

	public static void main(String[] args) throws Exception {
		GdxNativesLoader.load();
		Camera.main = new Camera(0, 0, 1, 1, 1);
		Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(), new Class<?>[]{Application.class},
				(proxy, method, values) -> {
					if (method.getReturnType() == boolean.class) return false;
					if (method.getReturnType() == int.class) return 0;
					return null;
				});
		// 命中图标在真实攻击入口初始化，读取项目资产但不创建图形窗口。
		Gdx.files = (Files) Proxy.newProxyInstance(Files.class.getClassLoader(), new Class<?>[]{Files.class},
				(proxy, method, values) -> {
					if (method.getName().equals("internal")) return new FileHandle(new File("core/src/main/assets", (String) values[0]));
					throw new UnsupportedOperationException(method.getName());
				});
		GameSettings.set((Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
				new Class<?>[]{Preferences.class}, (proxy, method, values) -> {
					if (method.getName().startsWith("get") && values != null && values.length == 2) return values[1];
					if (method.getReturnType() == boolean.class) return false;
					return null;
				}));
		System.out.println("场景 | 无领域 | 物理限定 | 魔法限定 | 物理与魔法失效（实际伤害）");
		try {
			matrix("英雄近战", Kind.PHYSICAL, (f, d) -> f.melee(f.target));
			matrix("英雄投掷", Kind.PHYSICAL, (f, d) -> f.shoot(new FixedStone()));
			matrix("英雄魔弹法杖", Kind.MAGIC, (f, d) -> f.wand(f.target));
			matrix("魔晶近战：物理本体＋魔法追加", Kind.MIXED, (f, d) -> {
				((WornShortsword) f.hero.belongings.weapon).enchant(new Crystal());
				return f.melee(f.target);
			});
			matrix("英雄普通炸弹", Kind.OTHER, (f, d) -> f.bomb(new Bomb(), f.hero));
			matrix("英雄奥术炸弹", Kind.MAGIC, (f, d) -> f.bomb(new ArcaneBomb(), f.hero));
			matrix("无施加者奥术炸弹", Kind.OTHER, (f, d) -> f.bomb(new ArcaneBomb(), null));
			matrix("中毒飞镖的持续中毒", Kind.OTHER, (f, d) -> {
				f.shoot(new PoisonDart());
				Poison poison = f.target.buff(Poison.class);
				check(poison != null, "飞镖实际命中后施加中毒，包括本体被屏蔽时");
				int before = f.target.HP;
				poison.act();
				return before - f.target.HP;
			});
			matrix("攻击引发的持续燃烧", Kind.OTHER, (f, d) -> {
				Buff.affect(f.hero, FireImbue.class).set(20);
				for (int i = 0; i < 16 && f.target.buff(Burning.class) == null; i++) f.melee(f.target);
				Burning burning = f.target.buff(Burning.class);
				check(burning != null, "真实近战火焰灌注施加燃烧");
				seed();
				int before = f.target.HP;
				burning.act();
				return before - f.target.HP;
			});
			matrix("镜像普通攻击：仅英雄带领域", Kind.OTHER, (f, d) -> f.mirror(Domain.NONE));
			matrix("镜像普通攻击：镜像也带领域", Kind.PHYSICAL, (f, d) -> f.mirror(d));
			matrix("哨卫射击：仅英雄带领域", Kind.OTHER, (f, d) -> f.ward(Domain.NONE));
			matrix("哨卫射击：哨卫也带领域", Kind.MAGIC, (f, d) -> f.ward(d));
			matrix("敌人近战打英雄", Kind.OTHER, (f, d) -> f.enemyAttack(false, Domain.NONE));
			matrix("敌人魔法打英雄", Kind.OTHER, (f, d) -> f.enemyAttack(true, Domain.NONE));
			matrix("敌人近战：敌人也带领域", Kind.PHYSICAL, (f, d) -> f.enemyAttack(false, d));
			matrix("敌人魔法：敌人也带领域", Kind.MAGIC, (f, d) -> f.enemyAttack(true, d));
			matrix("解离陷阱打敌人", Kind.OTHER, (f, d) -> f.trap(f.target));
			matrix("解离陷阱打英雄", Kind.OTHER, (f, d) -> f.trap(f.hero));
			matrix("物理攻击经生命链接分摊", Kind.PHYSICAL, (f, d) -> f.linked(false));
			matrix("魔法攻击经生命链接分摊", Kind.MAGIC, (f, d) -> f.linked(true));
			matrix("受害者带领域：英雄物理攻击", Kind.OTHER, (f, d) -> {
				f.clearHeroDomain(); d.apply(f.target); return f.melee(f.target);
			});
			matrix("受害者带领域：英雄魔法攻击", Kind.OTHER, (f, d) -> {
				f.clearHeroDomain(); d.apply(f.target); return f.wand(f.target);
			});
			shieldsAndDeferredDamage();
			lateDamageAndOwnership();
			statesAndDetach();
			System.out.println("DamageDomainRegression 通过：" + checks + " 项断言");
		} finally {
			Actor.clear();
			setField(GameScene.class, null, "scene", null);
			setField(Item.class, null, "curUser", null);
			Random.resetGenerators();
		}
	}

	private static void matrix(String label, Kind kind, Scenario scenario) throws Exception {
		int[] actual = new int[Domain.values().length];
		for (Domain domain : Domain.values()) {
			Fixture fixture = new Fixture();
			domain.apply(fixture.hero);
			seed();
			actual[domain.ordinal()] = scenario.run(fixture, domain);
		}
		check(actual[0] > 0, label + "：无领域必须真实造成伤害，避免空场景误通过");
		for (Domain domain : Domain.values()) {
			int expected = actual[0];
			if (kind == Kind.PHYSICAL && (domain == Domain.MAGIC || domain == Domain.NULL)) expected = 0;
			if (kind == Kind.MAGIC && (domain == Domain.PHYSICAL || domain == Domain.NULL)) expected = 0;
			if (kind == Kind.MIXED) {
				expected = domain == Domain.NONE ? 40 : domain == Domain.PHYSICAL ? 30 : domain == Domain.MAGIC ? 10 : 0;
			}
			check(actual[domain.ordinal()] == expected, label + "／" + domain.label
					+ "：预期伤害=" + expected + "，实际=" + actual[domain.ordinal()]);
		}
		System.out.println(label + " | " + actual[0] + " | " + actual[1] + " | " + actual[2] + " | " + actual[3]);
	}

	private static void shieldsAndDeferredDamage() throws Exception {
		for (Domain domain : Domain.values()) {
			Fixture f = new Fixture(); domain.apply(f.hero);
			Buff.affect(f.target, Barrier.class).setShield(100);
			f.melee(f.target);
			check(f.target.HP == f.target.HT, domain.label + "：护盾足够时不扣生命");
			check(f.target.shielding() == (domain == Domain.MAGIC || domain == Domain.NULL ? 100 : 70),
					domain.label + "：被屏蔽的物理本体不消耗护盾");

			f = new Fixture(); domain.apply(f.hero);
			Buff.affect(f.target, Viscosity.ViscosityTracker.class);
			int immediate = f.melee(f.target);
			Viscosity.DeferedDamage debt = f.target.buff(Viscosity.DeferedDamage.class);
			if (domain == Domain.MAGIC || domain == Domain.NULL) {
				check(immediate == 0 && debt == null, domain.label + "：被屏蔽的攻击不生成粘稠债务");
			} else {
				check(immediate == 25 && debt != null, domain.label + "：真实攻击产生25即时＋5延迟伤害");
				while (f.target.buff(Viscosity.DeferedDamage.class) != null) debt.act();
				check(f.target.HT - f.target.HP == 30, domain.label + "：允许的延迟伤害保持总额");
			}
		}
	}

	private static void lateDamageAndOwnership() throws Exception {
		for (Domain domain : Domain.values()) {
			Fixture f = new Fixture();
			Buff.affect(f.target, Viscosity.ViscosityTracker.class);
			f.melee(f.target);
			Viscosity.DeferedDamage debt = f.target.buff(Viscosity.DeferedDamage.class);
			domain.apply(f.hero);
			int before = f.target.HP;
			debt.act();
			boolean blocked = domain == Domain.MAGIC || domain == Domain.NULL;
			check(before - f.target.HP == (blocked ? 0 : 1), domain.label + "：延迟结算读取原攻击者当前领域");
			f.clearHeroDomain();
			while (f.target.buff(Viscosity.DeferedDamage.class) != null) debt.act();
			check(f.target.HT - f.target.HP == (blocked ? 29 : 30), domain.label + "：被屏蔽的延迟份额消退，不补发");

			f = new Fixture();
			ArcaneBomb bomb = new ArcaneBomb();
			setField(Bomb.class, bomb, "damageSourceId", f.hero.id());
			domain.apply(f.hero);
			Hero anotherUser = new FixedHero();
			anotherUser.pos = 13; Actor.add(anotherUser);
			setField(Item.class, null, "curUser", anotherUser);
			seed();
			int damage = f.detonate(bomb);
			check((damage > 0) == (domain == Domain.NONE || domain == Domain.MAGIC),
					domain.label + "：延迟魔法炸弹使用记录的施加者，不使用后来操作物品的角色");
		}
	}

	private static void statesAndDetach() throws Exception {
		for (Domain domain : Domain.values()) {
			Fixture f = new Fixture();
			Weakness existing = Buff.affect(f.hero, Weakness.class);
			domain.apply(f.hero);
			check(f.hero.buff(Weakness.class) == existing, domain.label + "：进入领域不清除已有魔法状态");
			check(Buff.affect(f.hero, Charm.class).target == f.hero, domain.label + "：领域不免疫新魅惑");
			check(Buff.affect(f.hero, Vulnerable.class).target == f.hero, domain.label + "：领域不免疫新易伤");
			f.clearHeroDomain();
			check(f.hero.buff(Weakness.class) == existing, domain.label + "：离开领域不清除状态");
			Buff.detach(f.hero, Weakness.class); Buff.detach(f.hero, Charm.class); Buff.detach(f.hero, Vulnerable.class);
			check(f.melee(f.target) == 30, domain.label + "：离开领域后物理攻击恢复");
			check(f.wand(f.target) > 0, domain.label + "：离开领域后魔法攻击恢复");
		}
	}

	private static class Fixture {
		final FixedHero hero = new FixedHero();
		final Target target = new Target();
		Fixture() throws Exception {
			Actor.clear(); seed();
			setField(GameScene.class, null, "scene", null);
			Dungeon.depth = 1; Dungeon.branch = 0; Dungeon.challenges = 0;
			Dungeon.hero = hero;
			TestLevel level = new TestLevel(); level.setSize(11, 11);
			level.blobs = new HashMap<>(); level.traps = new SparseArray<>();
			level.heaps = new SparseArray<>(); level.plants = new SparseArray<>();
			level.mobs = new HashSet<>(); level.transitions = new ArrayList<>();
			level.customTiles = new ArrayList<>(); level.customTerrain = new ArrayList<>(); level.customWalls = new ArrayList<>();
			for (int y = 1; y < 10; y++) for (int x = 1; x < 10; x++) level.map[y * 11 + x] = Terrain.EMPTY;
			level.buildFlagMaps(); Dungeon.level = level;
			hero.heroClass = HeroClass.WARRIOR; hero.STR = 10; hero.HP = hero.HT = 1000;
			hero.damageInterrupt = false;
			hero.pos = 60; hero.fieldOfView = new boolean[level.length()];
			Talent.initClassTalents(hero); hero.belongings.weapon = new FixedSword();
			target.pos = 61; target.HP = target.HT = 1000; target.alignment = Char.Alignment.ENEMY;
			Actor.add(hero); Actor.add(target);
			setField(Item.class, null, "curUser", hero);
		}
		void clearHeroDomain() {
			for (DamageDomain domain : hero.buffs(DamageDomain.class)) domain.detach();
		}
		int melee(Char victim) {
			int before = victim.HP;
			check(hero.attack(victim), "英雄真实近战命中");
			return before - victim.HP;
		}
		int shoot(MissileWeapon weapon) throws Exception {
			int before = target.HP;
			if (weapon instanceof PoisonDart) initParticles();
			try { check(hero.shoot(target, weapon), "英雄真实投掷攻击命中"); }
			finally { setField(GameScene.class, null, "scene", null); }
			return before - target.HP;
		}
		int wand(Char victim) {
			int before = victim.HP;
			new WandOfMagicMissile().onZap(new Ballistica(hero.pos, victim.pos, Ballistica.MAGIC_BOLT));
			return before - victim.HP;
		}
		int bomb(Bomb bomb, Char owner) throws Exception {
			setField(Bomb.class, bomb, "damageSourceId", owner == null ? -1 : owner.id());
			return detonate(bomb);
		}
		int detonate(Bomb bomb) throws Exception {
			int before = target.HP;
			if (bomb instanceof ArcaneBomb) {
				// 只提供空粒子池；爆炸范围、来源查找与扣血仍执行原实现。
				initParticles();
			}
			try { bomb.explode(target.pos); }
			finally { setField(GameScene.class, null, "scene", null); }
			return before - target.HP;
		}
		int mirror(Domain domain) {
			MirrorImage mirror = new MirrorImage(); mirror.duplicate(hero);
			mirror.pos = 62; mirror.sprite = new QuietSprite(); Actor.add(mirror);
			domain.apply(mirror);
			int before = target.HP;
			check(mirror.attack(target), "真实镜像普通攻击命中");
			return before - target.HP;
		}
		int ward(Domain domain) throws Exception {
			// 必须使用实际 Ward 类，不能让测试子类改变魔法名单的精确类判断。
			WandOfWarding.Ward ward = new WandOfWarding.Ward();
			ward.tier = 4; ward.HP = ward.HT = 1000; ward.pos = 63;
			ward.sprite = new QuietSprite(); ward.fieldOfView = new boolean[Dungeon.level.length()];
			setField(Mob.class, ward, "enemy", target); Actor.add(ward); domain.apply(ward);
			int before = target.HP;
			invoke(WandOfWarding.Ward.class, ward, "doAttack", target);
			return before - target.HP;
		}
		int enemyAttack(boolean magic, Domain domain) throws Exception {
			target.pos = 80; // 移开无关角色，保证电击弹道实际到达英雄。
			DM100 enemy = new DM100(); enemy.pos = magic ? 63 : 59;
			enemy.sprite = new QuietSprite(); Actor.add(enemy); domain.apply(enemy);
			int before = hero.HP;
			if (magic) invoke(DM100.class, enemy, "doAttack", hero);
			else check(enemy.attack(hero), "真实敌人近战命中");
			return before - hero.HP;
		}
		int trap(Char victim) {
			DisintegrationTrap trap = new DisintegrationTrap(); trap.pos = victim.pos;
			int before = victim.HP; trap.activate(); return before - victim.HP;
		}
		int linked(boolean magic) {
			Target linked = new Target(); linked.pos = 62; linked.HP = linked.HT = 1000;
			Actor.add(linked); Buff.affect(target, LifeLink.class).object = linked.id();
			int before = target.HP + linked.HP;
			if (magic) wand(target); else melee(target);
			check(target.HT - target.HP == linked.HT - linked.HP, "生命链接真实分摊给第二角色");
			return before - target.HP - linked.HP;
		}
	}

	public static class TestLevel extends Level {
		@Override protected boolean build() { return true; }
		@Override protected void createMobs() { }
		@Override protected void createItems() { }
	}
	private static class FixedHero extends Hero {
		{ sprite = new QuietSprite(); }
		@Override public int attackSkill(Char enemy) { return INFINITE_ACCURACY; }
		@Override public int defenseSkill(Char enemy) { return 0; }
		@Override public int drRoll() { return 0; }
	}
	private static class Target extends Char {
		{ sprite = new QuietSprite(); }
		@Override public int defenseSkill(Char enemy) { return 0; }
		@Override public int drRoll() { return 0; }
	}
	private static class FixedSword extends WornShortsword {
		@Override public int damageRoll(Char owner) { return 30; }
	}
	private static class FixedStone extends ThrowingStone {
		@Override public int damageRoll(Char owner) { return 18; }
	}
	private static class QuietSprite extends CharSprite {
		{ visible = false; }
		@Override public Emitter emitter() { return new QuietEmitter(); }
		@Override public void showStatusWithIcon(int color, String text, int icon, Object... args) { }
	}
	private static class QuietEmitter extends Emitter {
		@Override public void start(Factory factory, float interval, int quantity) { }
	}
	private static class QuietParticles extends Group {
		@Override public synchronized Gizmo recycle(Class<? extends Gizmo> type) { return new QuietEmitter(); }
	}
	private static void setField(Class<?> type, Object object, String name, Object value) throws Exception {
		Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(object, value);
	}
	private static void invoke(Class<?> type, Object object, String name, Char victim) throws Exception {
		Method method = type.getDeclaredMethod(name, Char.class); method.setAccessible(true); method.invoke(object, victim);
	}
	private static void seed() {
		Random.resetGenerators();
		Random.pushGenerator(SEED);
	}
	private static void initParticles() throws Exception {
		GameScene scene = new GameScene();
		setField(GameScene.class, scene, "emitters", new QuietParticles());
		setField(GameScene.class, null, "scene", scene);
	}
	private static void check(boolean condition, String message) {
		checks++;
		if (!condition) throw new AssertionError(message);
	}
}
