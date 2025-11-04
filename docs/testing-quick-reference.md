# Performance & Stress Tests - Quick Reference

## 📊 Test Matrix

| Test Type | Stage | Prod | Requests | Threshold | Pod Monitoring |
|-----------|-------|------|----------|-----------|----------------|
| **Performance - Health** | ✅ | ✅ | 100 / 30 | 95% | No |
| **Performance - Services** | ✅ | ✅ | 100 / 45 | 95% / 85% | No |
| **Performance - Response Time** | ❌ | ✅ | 0 / 20 | N/A | No |
| **Stress - Gradual** | ✅ | ❌ | ~75 | 90% | Yes |
| **Stress - Spike** | ✅ | ❌ | 50 | 90% | Yes |
| **Stress - Sustained** | ✅ | ❌ | ~300 | 90% | Yes |

## 🎯 Quick Commands

### Run in Dev Environment
```bash
./scripts/performance-stress-tests.sh ecommerce-dev all
```

### Run Performance Only
```bash
./scripts/performance-stress-tests.sh ecommerce-stage performance
```

### Run Stress Only
```bash
./scripts/performance-stress-tests.sh ecommerce-stage stress
```

## 📈 Success Criteria Summary

### Stage Environment
- **Performance:** ≥ 95% success rate (200 total requests)
- **Stress:** ≥ 90% success rate + 0 pod crashes

### Production Environment
- **Performance:** ≥ 95% health check + ≥ 85% routing (65 total requests)
- **Stress:** N/A (not run in production)

## 🔍 What Gets Tested

### Performance Tests
- ✅ Endpoint availability
- ✅ Response rate
- ✅ Throughput (req/sec)
- ✅ Success percentage
- ✅ Average response time (Prod only)

### Stress Tests (Stage Only)
- ✅ Concurrent request handling
- ✅ System stability under load
- ✅ Pod crash detection
- ✅ Restart count monitoring
- ✅ Sustained load capability

## 📝 Output Interpretation

### Good Result
```
✅ Performance tests completed!
📈 Overall Success Rate: 97.50%
⚡ Average Requests/sec: 8.33
```

### Failed Result
```
❌ Performance test failed: Success rate 92.00% is below 95%
exit 1
```

## 🚨 Common Failures

| Error | Cause | Solution |
|-------|-------|----------|
| Success rate < 95% | Service unhealthy | Check pod logs |
| Pod crashes | Resource limits | Increase memory/CPU |
| High response time | Overload | Scale replicas |
| Connection refused | Service not ready | Wait for startup |

## 📞 Files Reference

| File | Purpose |
|------|---------|
| `jenkins/Jenkinsfile` | Stage pipeline with full tests |
| `jenkins/Jenkinsfile-prod` | Prod pipeline with lighter tests |
| `scripts/performance-stress-tests.sh` | Standalone test script |
| `docs/performance-stress-testing.md` | Full documentation |

---

**Last Updated:** 3 de noviembre de 2025
