package ru.grouptable.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import ru.grouptable.entity.Person;

import java.util.List;

public class PersonDAO {
    private final SessionFactory sessionFactory;

    public PersonDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Person person) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.saveOrUpdate(person);
            tx.commit();
        }
    }

    public Person findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Person.class, id);
        }
    }

    public List<Person> getAllPersons() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Person", Person.class).list();
        }
    }

    public void delete(Person person) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.delete(person);
            tx.commit();
        }
    }

    public List<Person> findByNameLike(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return getAllPersons();
        }
        
        try (Session session = sessionFactory.openSession()) {
            String query = "FROM Person WHERE firstName LIKE :nameFilter";
            return session.createQuery(query, Person.class)
                .setParameter("nameFilter", "%" + searchText + "%")
                .list();
        }
    }

    public List<Person> findByFilters(String nameFilter, String fromDate, String toDate) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder queryBuilder = new StringBuilder("FROM Person WHERE 1=1");
            
            // Add name filter if provided
            if (nameFilter != null && !nameFilter.trim().isEmpty()) {
                queryBuilder.append(" AND firstName LIKE :nameFilter");
            }
            
            // Add date range filter if provided
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                queryBuilder.append(" AND birthDate >= :fromDate");
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                queryBuilder.append(" AND birthDate <= :toDate");
            }
            
            var query = session.createQuery(queryBuilder.toString(), Person.class);
            
            // Set parameters if they were added to query
            if (nameFilter != null && !nameFilter.trim().isEmpty()) {
                query.setParameter("nameFilter", "%" + nameFilter + "%");
            }
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                query.setParameter("fromDate", fromDate);
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                query.setParameter("toDate", toDate);
            }
            
            return query.list();
        }
    }
} 