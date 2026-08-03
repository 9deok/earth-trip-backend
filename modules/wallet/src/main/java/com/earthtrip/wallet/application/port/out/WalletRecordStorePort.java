package com.earthtrip.wallet.application.port.out;
import com.earthtrip.wallet.domain.WalletRecord;import java.util.*;
public interface WalletRecordStorePort{List<WalletRecord> findAll(UUID trip,String type,UUID parent);Optional<WalletRecord> findById(UUID id);WalletRecord save(WalletRecord r);}
