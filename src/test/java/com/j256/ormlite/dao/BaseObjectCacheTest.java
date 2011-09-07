package com.j256.ormlite.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

import com.j256.ormlite.BaseCoreTest;

public abstract class BaseObjectCacheTest extends BaseCoreTest {

	protected abstract ObjectCache enableCache(Dao<?, ?> dao);

	@Test
	public void testBasic() throws Exception {
		Dao<Foo, Object> dao = createDao(Foo.class, true);
		enableCache(dao);

		Foo foo = new Foo();
		String id = "hello";
		foo.id = id;
		int val = 12312321;
		foo.val = val;

		assertEquals(1, dao.create(foo));

		Foo result = dao.queryForId(id);
		assertSame(foo, result);

		List<Foo> results = dao.queryForAll();
		assertEquals(1, results.size());
		assertSame(result, results.get(0));

		// disable cache
		dao.enableObjectCache(null);

		result = dao.queryForId(id);
		assertNotSame(foo, result);
	}

	@Test
	public void testUpdate() throws Exception {
		Dao<Foo, Object> dao = createDao(Foo.class, true);
		enableCache(dao);

		Foo foo = new Foo();
		String id = "hello";
		foo.id = id;
		int val = 12312321;
		foo.val = val;
		assertEquals(1, dao.create(foo));

		Foo result = dao.queryForId(id);
		assertSame(foo, result);

		// update behind the back
		Foo foo2 = new Foo();
		foo2.id = id;
		int val2 = 1312341412;
		foo2.val = val2;
		assertEquals(1, dao.update(foo2));

		// the result should have the same value
		assertNotSame(foo, foo2);
		assertEquals(val2, foo.val);
	}

	@Test
	public void testUpdateId() throws Exception {
		Dao<Foo, Object> dao = createDao(Foo.class, true);
		enableCache(dao);

		Foo foo = new Foo();
		String id = "hello";
		foo.id = id;
		int val = 12312321;
		foo.val = val;
		assertEquals(1, dao.create(foo));

		Foo result = dao.queryForId(id);
		assertSame(foo, result);

		// updateId behind the back
		Foo foo2 = new Foo();
		foo2.id = id;
		int val2 = 1312341412;
		foo2.val = val2;
		String id2 = "jfpwojfe";
		assertEquals(1, dao.updateId(foo2, id2));

		// the result should _not_ have the same value
		assertNotSame(foo, foo2);
		// but the id should be the same
		assertEquals(id2, foo.id);
	}

	@Test
	public void testUpdateIdNotInCache() throws Exception {
		Dao<Foo, Object> dao = createDao(Foo.class, true);

		Foo foo = new Foo();
		String id = "hello";
		foo.id = id;
		int val = 12312321;
		foo.val = val;
		assertEquals(1, dao.create(foo));

		Foo result = dao.queryForId(id);
		assertNotSame(foo, result);

		// we enable the cache _after_ Foo was created
		ObjectCache cache = enableCache(dao);

		// updateId behind the back
		Foo foo2 = new Foo();
		foo2.id = id;
		int val2 = 1312341412;
		foo2.val = val2;
		String id2 = "jfpwojfe";
		assertEquals(1, dao.updateId(foo2, id2));

		// the result should _not_ have the same value
		assertNotSame(foo, foo2);
		// and the id should be the old one and not the new one
		assertEquals(id, foo.id);

		assertEquals(0, cache.size());
	}

	@Test
	public void testDelete() throws Exception {
		Dao<Foo, Object> dao = createDao(Foo.class, true);
		ObjectCache cache = enableCache(dao);

		Foo foo = new Foo();
		String id = "hello";
		foo.id = id;
		int val = 12312321;
		foo.val = val;
		assertEquals(1, dao.create(foo));

		Foo result = dao.queryForId(id);
		assertSame(foo, result);

		// updateId behind the back
		Foo foo2 = new Foo();
		foo2.id = id;

		assertEquals(1, cache.size());
		assertEquals(1, dao.delete(foo2));
		// foo still exists

		assertEquals(0, cache.size());
	}
}
