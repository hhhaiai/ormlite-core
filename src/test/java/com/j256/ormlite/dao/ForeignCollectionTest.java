package com.j256.ormlite.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.j256.ormlite.BaseCoreTest;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTableConfig;

public class ForeignCollectionTest extends BaseCoreTest {

	@Test
	public void testEagerCollection() throws Exception {
		Dao<Account, Integer> accountDao = createDao(Account.class, true);
		testCollection(accountDao, true);
	}

	@Test
	public void testLazyCollection() throws Exception {
		Dao<Account, Integer> accountDao = createLazyOrderDao();
		testCollection(accountDao, false);
	}

	@Test
	public void testForeignAutoRefreshWithCollection() throws Exception {
		createDao(Order.class, true);
		Dao<Account, Object> accountDao = createDao(Account.class, true);
		Dao<ForeignAutoRefresh, Object> farDao = createDao(ForeignAutoRefresh.class, true);
		Account account = new Account();
		String name1 = "fwepfjewfew";
		account.name = name1;
		assertEquals(1, accountDao.create(account));

		ForeignAutoRefresh far = new ForeignAutoRefresh();
		far.account = account;
		assertEquals(1, farDao.create(far));

		List<ForeignAutoRefresh> results = farDao.queryForAll();
		assertEquals(1, results.size());
		assertNotNull(results.get(0).account);
		assertNotNull(results.get(0).account.orders);
		assertEquals(0, results.get(0).account.orders.size());
	}

	@Test
	public void testQuestionAndAnswers() throws Exception {
		createDao(Order.class, true);
		Dao<Question, Object> questionDao = createDao(Question.class, true);
		Dao<Answer, Object> answerDao = createDao(Answer.class, true);

		Question question = new Question();
		String name = "some question";
		question.name = name;
		assertEquals(1, questionDao.create(question));

		Answer answer1 = new Answer();
		int val1 = 1234313123;
		answer1.val = val1;
		answer1.question = question;
		assertEquals(1, answerDao.create(answer1));

		Answer answer2 = new Answer();
		int val2 = 345543;
		answer2.val = val2;
		answer2.question = question;
		assertEquals(1, answerDao.create(answer2));

		assertEquals(1, questionDao.refresh(question));
		assertNotNull(question.answers);
		assertEquals(2, question.answers.size());
	}

	@Test
	public void testFieldOrder() throws Exception {
		Dao<AccountOrdered, Integer> accountDao = createDao(AccountOrdered.class, true);
		Dao<OrderOrdered, Integer> orderDao = createDao(OrderOrdered.class, true);

		AccountOrdered account1 = new AccountOrdered();
		String name1 = "fwepfjewfew";
		account1.name = name1;
		assertEquals(1, accountDao.create(account1));

		OrderOrdered order1 = new OrderOrdered();
		int val1 = 3;
		order1.val = val1;
		order1.account = account1;
		assertEquals(1, orderDao.create(order1));

		OrderOrdered order2 = new OrderOrdered();
		int val2 = 1;
		order2.val = val2;
		order2.account = account1;
		assertEquals(1, orderDao.create(order2));

		OrderOrdered order3 = new OrderOrdered();
		int val3 = 2;
		order3.val = val3;
		order3.account = account1;
		assertEquals(1, orderDao.create(order3));

		AccountOrdered accountResult = accountDao.queryForId(account1.id);
		assertEquals(name1, accountResult.name);
		assertNotNull(accountResult.orders);
		int orderC = 0;
		for (OrderOrdered order : accountResult.orders) {
			orderC++;
			switch (orderC) {
				case 1 :
					assertEquals(val2, order.val);
					break;
				case 2 :
					assertEquals(val3, order.val);
					break;
				case 3 :
					assertEquals(val1, order.val);
					break;
			}
		}
	}

	@Test(expected = SQLException.class)
	public void testNotProperCollection() throws Exception {
		createDao(NoProperCollection.class, true);
	}

	@Test(expected = SQLException.class)
	public void testNotParamaterized() throws Exception {
		createDao(NotParamaterized.class, true);
	}

	@Test(expected = SQLException.class)
	public void testNoForeignRelationship() throws Exception {
		createDao(NoForeign.class, true);
	}

	@Test
	public void testRecursiveReference() throws Exception {
		Dao<RecursiveReference, Object> dao = createDao(RecursiveReference.class, true);
		RecursiveReference rr1 = new RecursiveReference();
		rr1.stuff = "fpeewihwjytjythgwofjwe";
		assertEquals(1, dao.create(rr1));
		rr1.parent = rr1;
		assertEquals(1, dao.update(rr1));

		RecursiveReference result = dao.queryForId(rr1.id);
		assertNotNull(result);
		assertNotNull(result.related);
		assertFalse(result.related.isEager());
		assertEquals(1, result.related.size());
		CloseableIterator<RecursiveReference> iterator;

		// this would keep going forever since it is lazy
		for (int i = 0; i < 10; i++) {
			iterator = result.related.closeableIterator();
			assertTrue(iterator.hasNext());
			result = iterator.next();
			assertEquals(rr1.stuff, result.stuff);
			// Nth level is not null and is lazy
			assertNotNull(result.related);
			assertFalse(result.related.isEager());
			iterator.close();
		}
	}

	@Test
	public void testRecursiveReferenceEager() throws Exception {
		Dao<RecursiveReferenceEager, Object> dao = createDao(RecursiveReferenceEager.class, true);
		RecursiveReferenceEager rr1 = new RecursiveReferenceEager();
		rr1.stuff = "fpeewihwh13132gwofjwe";
		assertEquals(1, dao.create(rr1));
		rr1.parent = rr1;
		assertEquals(1, dao.update(rr1));

		RecursiveReferenceEager result = dao.queryForId(rr1.id);
		assertNotNull(result);
		// 0th level is not null and is eager
		assertNotNull(result.related);
		assertTrue(result.related.isEager());
		assertEquals(1, result.related.size());
		CloseableIterator<RecursiveReferenceEager> iterator = result.related.closeableIterator();
		assertTrue(iterator.hasNext());
		RecursiveReferenceEager rrResult = iterator.next();
		assertEquals(rr1.stuff, rrResult.stuff);
		// 1st level is not null but is lazy
		assertNotNull(rrResult.related);
		assertFalse(rrResult.related.isEager());
		assertFalse(iterator.hasNext());
		iterator.close();
	}

	@Test
	public void testRecursiveReferenceEagerTwo() throws Exception {
		Dao<RecursiveReferenceEagerLevelTwo, Object> dao = createDao(RecursiveReferenceEagerLevelTwo.class, true);
		RecursiveReferenceEagerLevelTwo rr1 = new RecursiveReferenceEagerLevelTwo();
		rr1.stuff = "fpeewifwfwehwhgwofjwe";
		assertEquals(1, dao.create(rr1));
		rr1.parent = rr1;
		assertEquals(1, dao.update(rr1));

		RecursiveReferenceEagerLevelTwo result = dao.queryForId(rr1.id);
		assertNotNull(result);
		// 0th level is not null and is eager
		assertNotNull(result.related);
		assertTrue(result.related.isEager());
		assertEquals(1, result.related.size());

		CloseableIterator<RecursiveReferenceEagerLevelTwo> iterator = result.related.closeableIterator();
		assertTrue(iterator.hasNext());
		RecursiveReferenceEagerLevelTwo rrResult = iterator.next();
		assertEquals(rr1.stuff, rrResult.stuff);
		// 1st level is not null and is eager
		assertNotNull(rrResult.related);
		assertTrue(rrResult.related.isEager());
		assertFalse(iterator.hasNext());
		iterator.close();
		iterator = rrResult.related.closeableIterator();
		assertTrue(iterator.hasNext());
		rrResult = iterator.next();
		assertFalse(iterator.hasNext());
		// but the 2nd level is not null but is lazy
		assertNotNull(rrResult.related);
		assertFalse(rrResult.related.isEager());
		iterator.close();
	}

	@Test
	public void testRecursiveReferenceEagerZero() throws Exception {
		/*
		 * No reason to do this in reality. You might as well say eager = false.
		 */
		Dao<RecursiveReferenceEagerLevelZero, Object> dao = createDao(RecursiveReferenceEagerLevelZero.class, true);
		RecursiveReferenceEagerLevelZero rr1 = new RecursiveReferenceEagerLevelZero();
		rr1.stuff = "fpeewifwfwehwhgwofjwe";
		assertEquals(1, dao.create(rr1));
		rr1.parent = rr1;
		assertEquals(1, dao.update(rr1));

		RecursiveReferenceEagerLevelZero result = dao.queryForId(rr1.id);
		assertNotNull(result);
		// 0th level is not null but is lazy
		assertNotNull(result.related);
		assertFalse(result.related.isEager());
	}

	@Test
	public void testCollectionsOfCollections() throws Exception {
		Dao<CollectionWithCollection1, Object> dao1 = createDao(CollectionWithCollection1.class, true);
		Dao<CollectionWithCollection2, Object> dao2 = createDao(CollectionWithCollection2.class, true);
		Dao<CollectionWithCollection3, Object> dao3 = createDao(CollectionWithCollection3.class, true);

		CollectionWithCollection1 coll1 = new CollectionWithCollection1();
		assertEquals(1, dao1.create(coll1));

		CollectionWithCollection2 coll2 = new CollectionWithCollection2();
		coll2.collectionWithCollection1 = coll1;
		assertEquals(1, dao2.create(coll2));

		CollectionWithCollection3 coll3 = new CollectionWithCollection3();
		coll3.collectionWithCollection2 = coll2;
		assertEquals(1, dao3.create(coll3));

		CollectionWithCollection1 collResult1 = dao1.queryForId(coll1.id);
		assertNotNull(collResult1.related);
		assertEquals(1, collResult1.related.size());
		CloseableIterator<CollectionWithCollection2> iterator1 = collResult1.related.closeableIterator();
		assertTrue(iterator1.hasNext());
		assertEquals(coll2.id, iterator1.next().id);
		collResult1.related.closeLastIterator();

		CollectionWithCollection2 collResult2 = dao2.queryForId(coll2.id);
		assertNotNull(collResult2.related);
		assertEquals(1, collResult2.related.size());
		CloseableIterator<CollectionWithCollection3> iterator2 = collResult2.related.closeableIterator();
		assertTrue(iterator2.hasNext());
		assertEquals(coll3.id, iterator2.next().id);
		collResult2.related.closeLastIterator();
	}

	@Test
	public void testEmptyCollection() throws Exception {
		Dao<Account, Object> accountDao = createDao(Account.class, true);
		createTable(Order.class, true);

		Account account = new Account();
		String name = "another name";
		account.name = name;
		account.orders = accountDao.getEmptyForeignCollection(Account.ORDERS_FIELD_NAME);
		assertEquals(1, accountDao.create(account));

		Order order = new Order();
		int val3 = 17097;
		order.val = val3;
		order.account = account;
		account.orders.add(order);
		assertEquals(1, account.orders.size());

		Account accountResult = accountDao.queryForId(account.id);
		assertNotNull(accountResult.orders);
		assertEquals(1, accountResult.orders.size());
		CloseableIterator<Order> iterator = accountResult.orders.closeableIterator();
		assertTrue(iterator.hasNext());
		Order orderResult = iterator.next();
		assertNotNull(orderResult);
		assertEquals(order.id, orderResult.id);
		assertFalse(iterator.hasNext());
		iterator.close();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownEmptyCollection() throws Exception {
		Dao<Account, Object> dao = createDao(Account.class, true);
		dao.getEmptyForeignCollection("unknown field name");
	}

	@Test
	public void testLazyContainsAll() throws Exception {
		Dao<Account, Integer> accountDao = createLazyOrderDao();
		Dao<Order, Integer> orderDao = createDao(Order.class, true);

		Account account1 = new Account();
		String name1 = "fwepfjewfew";
		account1.name = name1;
		assertEquals(1, accountDao.create(account1));

		Order order1 = new Order();
		int val1 = 13123441;
		order1.val = val1;
		order1.account = account1;
		assertEquals(1, orderDao.create(order1));

		Order order2 = new Order();
		int val2 = 113787097;
		order2.val = val2;
		order2.account = account1;
		assertEquals(1, orderDao.create(order2));

		Order order3 = new Order();
		int val3 = 1524587097;
		order3.val = val3;
		order3.account = account1;

		Account result = accountDao.queryForId(account1.id);
		assertEquals(2, result.orders.size());

		assertTrue(result.orders.containsAll(Arrays.asList()));
		assertTrue(result.orders.containsAll(Arrays.asList(order1)));
		assertTrue(result.orders.containsAll(Arrays.asList(order1, order2)));
		assertTrue(result.orders.containsAll(Arrays.asList(order2, order1)));
		assertTrue(result.orders.containsAll(Arrays.asList(order1, order1)));
		assertTrue(result.orders.containsAll(Arrays.asList(order1, order1, order2, order2)));
		assertFalse(result.orders.containsAll(Arrays.asList(order1, order2, order3)));
		assertFalse(result.orders.containsAll(Arrays.asList(order3)));
		assertFalse(result.orders.containsAll(Arrays.asList(order3, order1, order2)));
	}

	/* =============================================================================================== */

	private void testCollection(Dao<Account, Integer> accountDao, boolean eager) throws Exception {
		Dao<Order, Integer> orderDao = createDao(Order.class, true);

		Account accountOther = new Account();
		String nameOther = "fwepfjewfew";
		accountOther.name = nameOther;
		assertEquals(1, accountDao.create(accountOther));

		Order orderOther = new Order();
		int valOther = 1453783141;
		orderOther.val = valOther;
		orderOther.account = accountOther;
		assertEquals(1, orderDao.create(orderOther));

		Account account1 = new Account();
		String name1 = "fwepfjewfew";
		account1.name = name1;
		assertEquals(1, accountDao.create(account1));

		Order order1 = new Order();
		int val1 = 13123441;
		order1.val = val1;
		order1.account = account1;
		assertEquals(1, orderDao.create(order1));

		Order order2 = new Order();
		int val2 = 113787097;
		order2.val = val2;
		order2.account = account1;
		assertEquals(1, orderDao.create(order2));

		// insert some other stuff just to confuse matters
		Account account2 = new Account();
		String name2 = "another name";
		account2.name = name2;
		assertEquals(1, accountDao.create(account2));

		Order order3 = new Order();
		int val3 = 17097;
		order3.val = val3;
		order3.account = account2;
		assertEquals(1, orderDao.create(order3));

		Account accountResult = accountDao.queryForId(account1.id);
		assertEquals(name1, accountResult.name);
		assertNotNull(accountResult.orders);
		int orderC = 0;
		for (Order order : accountResult.orders) {
			orderC++;
			switch (orderC) {
				case 1 :
					assertEquals(val1, order.val);
					break;
				case 2 :
					assertEquals(val2, order.val);
					break;
			}
			assertSame(accountResult, order.account);
		}
		assertEquals(2, orderC);
		assertFalse(accountResult.orders.isEmpty());
		assertTrue(accountResult.orders.contains(order1));
		assertTrue(accountResult.orders.containsAll(Arrays.asList(order1, order2)));
		Object[] objs = accountResult.orders.toArray();
		assertNotNull(objs);
		assertEquals(2, objs.length);
		assertEquals(order1, objs[0]);
		assertEquals(order2, objs[1]);
		Order[] orders = accountResult.orders.toArray(new Order[0]);
		assertNotNull(orders);
		assertEquals(2, orders.length);
		assertEquals(order1, orders[0]);
		assertEquals(order2, orders[1]);
		orders = accountResult.orders.toArray(new Order[1]);
		assertNotNull(orders);
		assertEquals(2, orders.length);
		assertEquals(order1, orders[0]);
		assertEquals(order2, orders[1]);
		orders = accountResult.orders.toArray(new Order[2]);
		assertNotNull(orders);
		assertEquals(2, orders.length);
		assertEquals(order1, orders[0]);
		assertEquals(order2, orders[1]);
		orders = accountResult.orders.toArray(new Order[3]);
		assertNotNull(orders);
		assertEquals(3, orders.length);
		assertEquals(order1, orders[0]);
		assertEquals(order2, orders[1]);
		assertNull(orders[2]);

		CloseableWrappedIterable<Order> wrapped = accountResult.orders.getWrappedIterable();
		try {
			orderC = 0;
			for (Order order : wrapped) {
				orderC++;
				switch (orderC) {
					case 1 :
						assertEquals(val1, order.val);
						break;
					case 2 :
						assertEquals(val2, order.val);
						break;
				}
				assertSame(accountResult, order.account);
			}
			assertEquals(2, orderC);
		} finally {
			wrapped.close();
		}
		
		// insert it via the collection
		Order order5 = new Order();
		int val5 = 76557654;
		order5.val = val5;
		order5.account = account1;
		accountResult.orders.add(order5);
		// the size should change immediately
		assertEquals(3, accountResult.orders.size());

		// now insert it behind the collections back
		Order order6 = new Order();
		int val6 = 1123587097;
		order6.val = val6;
		order6.account = account1;
		assertEquals(1, orderDao.create(order6));
		if (eager) {
			// account2's collection should not have changed
			assertEquals(3, accountResult.orders.size());
		} else {
			// lazy does another query
			assertEquals(4, accountResult.orders.size());
		}

		// now we refresh the collection
		assertEquals(1, accountDao.refresh(accountResult));
		assertEquals(name1, accountResult.name);
		assertNotNull(accountResult.orders);
		orderC = 0;
		for (Order order : accountResult.orders) {
			orderC++;
			switch (orderC) {
				case 1 :
					assertEquals(val1, order.val);
					break;
				case 2 :
					assertEquals(val2, order.val);
					break;
				case 3 :
					assertEquals(val5, order.val);
					break;
				case 4 :
					assertEquals(val6, order.val);
					break;
			}
		}
		assertEquals(4, orderC);

		assertTrue(accountResult.orders.remove(order5));
		assertEquals(3, accountResult.orders.size());
		assertTrue(accountResult.orders.removeAll(Arrays.asList(order5, order6)));
		assertEquals(2, accountResult.orders.size());
		assertEquals(1, accountDao.refresh(accountResult));
		orderC = 0;
		for (Order order : accountResult.orders) {
			orderC++;
			switch (orderC) {
				case 1 :
					assertEquals(val1, order.val);
					break;
				case 2 :
					assertEquals(val2, order.val);
					break;
			}
		}
		assertEquals(2, orderC);

		assertTrue(accountResult.orders.retainAll(Arrays.asList(order1)));
		orderC = 0;
		for (Order order : accountResult.orders) {
			orderC++;
			switch (orderC) {
				case 1 :
					assertEquals(val1, order.val);
					break;
			}
		}
		assertEquals(1, orderC);

		CloseableIterator<Order> iterator = accountResult.orders.closeableIterator();
		assertTrue(iterator.hasNext());
		assertEquals(order1, iterator.next());
		iterator.remove();
		assertFalse(iterator.hasNext());
		iterator.close();
		assertEquals(0, accountResult.orders.size());

		accountResult.orders.addAll(Arrays.asList(order1, order2, order5, order6));

		orderC = 0;
		boolean gotOrder5 = false;
		boolean gotOrder6 = false;
		for (Order order : accountResult.orders) {
			orderC++;
			switch (orderC) {
				case 1 :
					assertEquals(val1, order.val);
					break;
				case 2 :
					assertEquals(val2, order.val);
					break;
				case 3 :
					if ((!gotOrder5) && order.val == val5) {
						gotOrder5 = true;
					} else if ((!gotOrder6) && order.val == val6) {
						gotOrder6 = true;
					} else {
						fail("Should have gotten order5 or order6");
					}
					break;
				case 4 :
					if ((!gotOrder5) && order.val == val5) {
						gotOrder5 = true;
					} else if ((!gotOrder6) && order.val == val6) {
						gotOrder6 = true;
					} else {
						fail("Should have gotten order5 or order6");
					}
					break;
			}
		}
		assertEquals(4, orderC);
		assertTrue(gotOrder5);
		assertTrue(gotOrder6);

		accountResult.orders.clear();
		assertEquals(0, accountResult.orders.size());

		orders = accountResult.orders.toArray(new Order[2]);
		assertNotNull(orders);
		assertEquals(2, orders.length);
		assertNull(orders[0]);
		assertNull(orders[1]);

		assertFalse(accountResult.orders.contains(order1));
		assertFalse(accountResult.orders.containsAll(Arrays.asList(order1, order2, order5, order6)));

		assertEquals(name1, accountResult.name);
		String name3 = "gjrogejroregjpo";
		accountResult.name = name3;
		assertEquals(1, accountDao.update(accountResult));

		accountResult = accountDao.queryForId(account1.id);
		assertNotNull(accountResult);
		assertEquals(name3, accountResult.name);

		int newId = account1.id + 100;
		assertEquals(1, accountDao.updateId(accountResult, newId));
		accountResult = accountDao.queryForId(newId);
		assertNotNull(accountResult);

		assertEquals(1, accountDao.delete(accountResult));
		accountResult = accountDao.queryForId(newId);
		assertNull(accountResult);

		Account result = accountDao.queryForId(accountOther.id);
		iterator = result.orders.closeableIterator();
		assertTrue(iterator.hasNext());
		assertEquals(valOther, iterator.next().val);
		assertFalse(iterator.hasNext());
		iterator.close();
	}

	private Dao<Account, Integer> createLazyOrderDao() throws SQLException, Exception {
		List<DatabaseFieldConfig> fieldConfigs = new ArrayList<DatabaseFieldConfig>();
		for (Field field : Account.class.getDeclaredFields()) {
			DatabaseFieldConfig fieldConfig = DatabaseFieldConfig.fromField(databaseType, "account", field);
			if (fieldConfig != null) {
				if (fieldConfig.isForeignCollection()) {
					fieldConfig.setForeignCollectionEager(false);
				}
				fieldConfigs.add(fieldConfig);
			}
		}
		DatabaseTableConfig<Account> tableConfig = new DatabaseTableConfig<Account>(Account.class, fieldConfigs);
		Dao<Account, Integer> accountDao = createDao(tableConfig, true);
		return accountDao;
	}

	/* =============================================================================================== */

	protected static class Account {
		public static final String ORDERS_FIELD_NAME = "orders123";
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String name;
		@ForeignCollectionField(eager = true, columnName = ORDERS_FIELD_NAME)
		ForeignCollection<Order> orders;
		protected Account() {
		}
	}

	protected static class Order {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField(unique = true)
		int val;
		@DatabaseField(foreign = true)
		Account account;
		protected Order() {
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			return id == ((Order) obj).id;
		}
		@Override
		public int hashCode() {
			return id;
		}
	}

	protected static class Question {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String name;
		@DatabaseField(foreign = true, foreignAutoRefresh = true)
		Answer bestAnswer;
		@ForeignCollectionField(eager = true)
		ForeignCollection<Answer> answers;
		protected Question() {
		}
	}

	protected static class Answer {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField(unique = true)
		int val;
		@DatabaseField(foreign = true, foreignAutoRefresh = true)
		Question question;
		protected Answer() {
		}
	}

	protected static class NoProperCollection {
		@DatabaseField(generatedId = true)
		int id;
		@ForeignCollectionField(eager = true)
		List<Order> orders;
		protected NoProperCollection() {
		}
	}

	protected static class NotParamaterized {
		@DatabaseField(generatedId = true)
		int id;
		@SuppressWarnings("rawtypes")
		@ForeignCollectionField(eager = true)
		Collection orders;
		protected NotParamaterized() {
		}
	}

	protected static class NoForeign {
		@DatabaseField(generatedId = true)
		int id;
		@ForeignCollectionField(eager = true)
		Collection<Order> orders;
		protected NoForeign() {
		}
	}

	protected static class ForeignAutoRefresh {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField(foreign = true, foreignAutoRefresh = true)
		Account account;
		protected ForeignAutoRefresh() {
		}
	}

	protected static class RecursiveReference {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String stuff;
		@DatabaseField(foreign = true)
		RecursiveReference parent;
		@ForeignCollectionField
		ForeignCollection<RecursiveReference> related;
		protected RecursiveReference() {
		}
	}

	protected static class RecursiveReferenceEager {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String stuff;
		@DatabaseField(foreign = true)
		RecursiveReferenceEager parent;
		@ForeignCollectionField(eager = true)
		ForeignCollection<RecursiveReferenceEager> related;
		protected RecursiveReferenceEager() {
		}
	}

	protected static class RecursiveReferenceEagerLevelTwo {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String stuff;
		@DatabaseField(foreign = true)
		RecursiveReferenceEagerLevelTwo parent;
		@ForeignCollectionField(eager = true, maxEagerForeignCollectionLevel = 2)
		ForeignCollection<RecursiveReferenceEagerLevelTwo> related;
		protected RecursiveReferenceEagerLevelTwo() {
		}
	}

	protected static class RecursiveReferenceEagerLevelZero {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String stuff;
		@DatabaseField(foreign = true)
		RecursiveReferenceEagerLevelZero parent;
		@ForeignCollectionField(eager = true, maxEagerForeignCollectionLevel = 0)
		ForeignCollection<RecursiveReferenceEagerLevelZero> related;
		protected RecursiveReferenceEagerLevelZero() {
		}
	}

	protected static class CollectionWithCollection1 {
		@DatabaseField(generatedId = true)
		int id;
		@ForeignCollectionField
		ForeignCollection<CollectionWithCollection2> related;
		protected CollectionWithCollection1() {
		}
	}

	protected static class CollectionWithCollection2 {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField(foreign = true)
		CollectionWithCollection1 collectionWithCollection1;
		@ForeignCollectionField
		ForeignCollection<CollectionWithCollection3> related;
		protected CollectionWithCollection2() {
		}
	}

	protected static class CollectionWithCollection3 {
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField(foreign = true)
		CollectionWithCollection2 collectionWithCollection2;
		protected CollectionWithCollection3() {
		}
	}

	protected static class AccountOrdered {
		public static final String ORDERS_FIELD_NAME = "orders123";
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField
		String name;
		@ForeignCollectionField(eager = true, columnName = ORDERS_FIELD_NAME, orderColumnName = OrderOrdered.VAL_FIELD_NAME)
		ForeignCollection<OrderOrdered> orders;
		protected AccountOrdered() {
		}
	}

	protected static class OrderOrdered {
		public final static String VAL_FIELD_NAME = "val";
		@DatabaseField(generatedId = true)
		int id;
		@DatabaseField(unique = true, columnName = VAL_FIELD_NAME)
		int val;
		@DatabaseField(foreign = true)
		AccountOrdered account;
		protected OrderOrdered() {
		}
	}
}
