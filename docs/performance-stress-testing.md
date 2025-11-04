# Performance and Stress Testing Documentation

## Overview

This document describes the implementation of performance and stress tests for the e-commerce microservices platform. These tests are integrated into the Jenkins CI/CD pipelines for Stage and Production environments.

## Test Types

### 1. Performance Tests

Performance tests validate that the system meets response time and throughput requirements under normal load conditions.

#### Stage Environment Tests

**Test 1: API Gateway Health Endpoint**
- **Requests:** 100 sequential requests
- **Target:** `/actuator/health`
- **Success Criteria:** 95% success rate
- **Metrics Collected:**
  - Success/Failure count
  - Success rate percentage
  - Requests per second
  - Total duration

**Test 2: User Service Endpoint**
- **Requests:** 50 sequential requests
- **Target:** `/api/users/actuator/health`
- **Success Criteria:** Included in overall 95% threshold
- **Metrics Collected:** Same as Test 1

**Test 3: Product Service Endpoint**
- **Requests:** 50 sequential requests
- **Target:** `/api/products/actuator/health`
- **Success Criteria:** Included in overall 95% threshold
- **Metrics Collected:** Same as Test 1

**Overall Threshold:** 95% success rate across all tests

#### Production Environment Tests (Lighter)

**Test 1: API Gateway Health**
- **Requests:** 30 sequential requests (lighter load)
- **Target:** `/actuator/health`
- **Success Criteria:** 95% success rate
- **Metrics Collected:**
  - Success rate
  - Average response time (ms)
  - Total duration

**Test 2: Service Routing Validation**
- **Services Tested:** users, products, orders
- **Requests:** 15 per service (45 total)
- **Success Criteria:** 85% overall success rate
- **Metrics:** Individual and overall success rates

**Test 3: Response Time Thresholds**
- **Requests:** 20 requests
- **Thresholds:**
  - Fast: < 500ms
  - Acceptable: 500-1000ms
  - Slow: > 1000ms
- **Reporting:** Distribution of response times

### 2. Stress Tests (Stage Only)

Stress tests validate system stability under heavy and variable load conditions.

**Test 1: Gradual Load Increase**
- **Pattern:** Incremental concurrent requests (1 → 5 → 10 → 15 → 20)
- **Duration:** ~10 seconds per level
- **Validation:**
  - Success/failure count at each level
  - Pod stability (no crashes)
  - Response time degradation

**Test 2: Spike Test**
- **Pattern:** Sudden burst of 50 concurrent requests
- **Phases:**
  - Warmup: 5 sequential requests
  - Spike: 50 concurrent requests
  - Cooldown: 5 second wait
- **Validation:**
  - Success rate during spike
  - Pod count before/after spike
  - No cascading failures

**Test 3: Sustained Load**
- **Pattern:** Continuous load over 30 seconds
- **Concurrency:** 10 concurrent requests per iteration
- **Validation:**
  - Overall success rate > 90%
  - System stability (no pod crashes)
  - Restart count within limits

**Overall Threshold:** 90% success rate, no pod failures

## Implementation Details

### Technology Stack

- **Testing Tool:** Bash scripts with `curl` and `kubectl`
- **Metrics Collection:** Shell arithmetic and `awk` for calculations
- **Pod Monitoring:** `kubectl` for checking pod health
- **CI/CD Integration:** Jenkins Pipeline stages

### Key Features

1. **Real-time Metrics:**
   - Success/failure counts
   - Response times
   - Requests per second
   - Success rate percentages

2. **System Health Monitoring:**
   - Pod count tracking
   - Restart count monitoring
   - Crash detection

3. **Threshold Validation:**
   - Automatic pass/fail based on criteria
   - Pipeline fails if thresholds not met

4. **Detailed Reporting:**
   - Test-by-test breakdown
   - Overall summary with key metrics
   - Visual formatting for clarity

## Jenkins Pipeline Integration

### Stage Pipeline (Jenkinsfile / Jenkinsfile-dev)

```groovy
stage('Performance Tests') {
    steps {
        script {
            // Runs full performance test suite
            // 200 total requests across 3 endpoints
            // Requires 95% success rate
        }
    }
}

stage('Stress Tests') {
    steps {
        script {
            // Runs all 3 stress tests
            // Monitors system stability
            // Requires 90% success rate, no crashes
        }
    }
}
```

### Production Pipeline (Jenkinsfile-prod)

```groovy
stage('Performance Tests') {
    steps {
        script {
            // Lighter performance validation
            // 65 total requests
            // More lenient thresholds (95% health, 85% routing)
        }
    }
}

// Note: No stress tests in production
```

## Standalone Script Usage

A standalone script is available for manual testing:

```bash
# Location
scripts/performance-stress-tests.sh

# Usage
./scripts/performance-stress-tests.sh <namespace> [test-type]

# Examples
./scripts/performance-stress-tests.sh ecommerce-dev all
./scripts/performance-stress-tests.sh ecommerce-stage performance
./scripts/performance-stress-tests.sh ecommerce-prod performance
```

### Parameters

- **namespace:** Kubernetes namespace (e.g., ecommerce-dev, ecommerce-stage)
- **test-type:** `performance`, `stress`, or `all` (default: all)

## Metrics and Thresholds

### Performance Test Thresholds

| Environment | Success Rate | Requests | Duration |
|------------|--------------|----------|----------|
| Stage      | ≥ 95%        | 200      | ~10-20s  |
| Production | ≥ 95% (health) / ≥ 85% (routing) | 65 | ~5-10s |

### Stress Test Thresholds (Stage Only)

| Test | Threshold | Metric |
|------|-----------|--------|
| Gradual Load | Success rate tracked | Pod stability |
| Spike Test | Success rate reported | No crashes |
| Sustained Load | ≥ 90% success | No pod failures |

### System Health Thresholds

- **Pod Crashes:** 0 (any crash = failure)
- **Restart Count:** ≤ 5 warning threshold
- **Pod Count:** Must remain stable (no decrease)

## Output Examples

### Performance Test Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Test 1: API Gateway Health Endpoint (100 requests)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✓ Completed: 100 requests in 12s
  ✓ Success: 98 | Failed: 2
  ✓ Success Rate: 98.00%
  ✓ Requests/sec: 8.17

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ PERFORMANCE TESTS SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  📊 Total Requests: 200
  ✅ Successful: 195
  ❌ Failed: 5
  📈 Overall Success Rate: 97.50%
  ⚡ Average Requests/sec: 8.33
  ⏱️  Total Duration: 23s

✅ All performance tests passed!
```

### Stress Test Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Stress Test 1: Gradual Load Increase (1→10→20→30 concurrent)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  → Testing with 1 concurrent requests...
     ✓ 1 concurrent: 1 success, 0 failed in 1s
  → Testing with 5 concurrent requests...
     ✓ 5 concurrent: 5 success, 0 failed in 2s
  → Testing with 10 concurrent requests...
     ✓ 10 concurrent: 10 success, 0 failed in 3s

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ STRESS TESTS SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🔍 System Stability Check:
     Initial Pods: 8
     Final Pods: 8
     Total Restarts: 0

  ✅ System remained stable under stress
  ✅ No pod crashes detected
  ✅ Success rate: 96.50% (threshold: 90%)

✅ All stress tests passed!
```

## Best Practices

### For Development

1. Run performance tests after major changes
2. Monitor trends over time
3. Investigate any degradation in metrics

### For Stage

1. Run full test suite before promoting to production
2. Performance tests validate normal operations
3. Stress tests validate resilience

### For Production

1. Use lighter performance tests to avoid impact
2. No stress tests (potential service disruption)
3. Monitor metrics closely

## Troubleshooting

### Common Issues

**Issue: Low Success Rate**
- Check service health: `kubectl get pods -n <namespace>`
- Check logs: `kubectl logs -n <namespace> <pod-name>`
- Verify network connectivity

**Issue: High Response Times**
- Check resource utilization: `kubectl top pods -n <namespace>`
- Verify pod limits and requests
- Check for bottlenecks in services

**Issue: Pod Crashes During Tests**
- Review pod logs before crash
- Check resource limits (memory, CPU)
- Investigate application errors
- Consider increasing resources

**Issue: Timeout Errors**
- Verify services are responding
- Check for long-running operations
- Increase timeout thresholds if appropriate

## Future Enhancements

1. **Advanced Tools:**
   - Integrate Apache JMeter for complex scenarios
   - Add Gatling for detailed reports
   - Use k6 for modern load testing

2. **Metrics:**
   - Collect and store historical data
   - Create dashboards (Grafana)
   - Alert on threshold violations

3. **Test Scenarios:**
   - Add API functional tests
   - Test complete user journeys
   - Add database stress tests
   - Test failure scenarios

4. **Automation:**
   - Scheduled performance testing
   - Automatic threshold adjustment
   - Regression detection

## References

- Jenkins Pipeline Documentation
- Kubernetes Best Practices
- Performance Testing Strategies
- Site Reliability Engineering (SRE) principles

---

**Last Updated:** 3 de noviembre de 2025  
**Author:** Alejandro Castro
