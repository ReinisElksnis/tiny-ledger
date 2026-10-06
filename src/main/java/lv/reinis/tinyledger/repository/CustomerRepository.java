package lv.reinis.tinyledger.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.reinis.tinyledger.domain.Customer;


public interface CustomerRepository extends JpaRepository<Customer, UUID>
{
}
