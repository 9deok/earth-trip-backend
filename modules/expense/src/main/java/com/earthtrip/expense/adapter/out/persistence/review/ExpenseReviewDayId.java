package com.earthtrip.expense.adapter.out.persistence.review;

import java.io.Serializable;
import java.time.LocalDate;

record ExpenseReviewDayId(String tripId, LocalDate localDate) implements Serializable { }
