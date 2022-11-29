package ru.springLearning.SpringDemoBot.model;

import org.springframework.data.repository.CrudRepository;

public interface AdsRepository extends CrudRepository<Ads, Long> {
}
