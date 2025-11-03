# Jenkins GitHub Webhook Setup Guide

This guide explains how to configure automatic Jenkins builds when commits are pushed to the `dev` branch.

## Overview

The Jenkinsfile now includes two automatic trigger mechanisms:
1. **GitHub Webhook** - Immediate build trigger on push (recommended)
2. **SCM Polling** - Checks for changes every 5 minutes (backup)

## Prerequisites

- Jenkins server accessible from the internet (or GitHub can reach it)
- GitHub repository: `AIejoCastro/ecommerce-microservice-backend-app`
- Jenkins job configured for the repository
- Admin access to the GitHub repository

## Step 1: Install Jenkins GitHub Plugin

1. Go to Jenkins Dashboard → **Manage Jenkins** → **Manage Plugins**
2. Go to **Available** tab
3. Search for "GitHub Plugin"
4. Install **GitHub Plugin** and **GitHub Integration Plugin**
5. Restart Jenkins if required

## Step 2: Configure GitHub Webhook

### Option A: Using GitHub UI (Recommended)

1. Go to your GitHub repository: https://github.com/AIejoCastro/ecommerce-microservice-backend-app

2. Navigate to **Settings** → **Webhooks** → **Add webhook**

3. Configure the webhook:
   ```
   Payload URL: http://YOUR_JENKINS_URL:8080/github-webhook/
   Content type: application/json
   Secret: (leave empty or add a token for security)
   SSL verification: Enable SSL verification (if using HTTPS)
   ```
   
   **Example:**
   ```
   Payload URL: http://localhost:8080/github-webhook/
   ```
   
   > **Note:** If Jenkins is running on your local machine, you'll need to expose it using:
   > - ngrok: `ngrok http 8080`
   > - localtunnel: `lt --port 8080`
   > - GitHub Actions (alternative approach)

4. **Which events would you like to trigger this webhook?**
   - Select **Just the push event**

5. Ensure **Active** is checked

6. Click **Add webhook**

### Option B: Using Jenkins Multibranch Pipeline

If using a Multibranch Pipeline:

1. In Jenkins, go to your pipeline job configuration
2. Under **Branch Sources** → **GitHub**
3. Add your repository credentials
4. Configure **Scan Repository Triggers**:
   - Check **Periodically if not otherwise run**
   - Set interval (e.g., 5 minutes)

## Step 3: Configure Jenkins Job

1. Go to your Jenkins job → **Configure**

2. Under **Build Triggers**, ensure these are enabled:
   - ✅ **GitHub hook trigger for GITScm polling**
   - ✅ **Poll SCM** (as backup) - set to `H/5 * * * *`

3. Under **Source Code Management** → **Git**:
   ```
   Repository URL: https://github.com/AIejoCastro/ecommerce-microservice-backend-app.git
   Branches to build: */dev
   ```

4. Save the configuration

## Step 4: Test the Webhook

1. Make a small change to your repository on the `dev` branch
2. Commit and push:
   ```bash
   git add .
   git commit -m "Test: Trigger Jenkins webhook"
   git push origin dev
   ```

3. Check Jenkins:
   - Go to your Jenkins job
   - You should see a new build triggered automatically within seconds
   - Check **Console Output** to verify it was triggered by the webhook

4. Verify the webhook delivery in GitHub:
   - Go to **Settings** → **Webhooks** → Click on your webhook
   - Check **Recent Deliveries** tab
   - Should show successful delivery (green checkmark)

## Troubleshooting

### Jenkins Not Receiving Webhook

**Issue:** GitHub webhook shows successful but Jenkins doesn't trigger

**Solutions:**
1. Check Jenkins URL is accessible from internet:
   ```bash
   curl http://YOUR_JENKINS_URL:8080/github-webhook/
   ```

2. Verify Jenkins GitHub Plugin is installed:
   - Go to **Manage Jenkins** → **Manage Plugins** → **Installed**
   - Look for "GitHub Plugin"

3. Check Jenkins system log:
   - **Manage Jenkins** → **System Log**
   - Look for webhook-related errors

### Webhook Failing with 403/404

**Issue:** GitHub webhook delivery fails

**Solutions:**
1. Ensure URL includes trailing slash: `/github-webhook/`
2. Check Jenkins CSRF protection settings
3. Verify no authentication required for webhook endpoint

### Local Jenkins Not Reachable from GitHub

**Issue:** Jenkins running on localhost can't receive GitHub webhooks

**Solutions:**

#### Option 1: Use ngrok (Quick & Easy)
```bash
# Install ngrok
brew install ngrok  # macOS

# Expose Jenkins
ngrok http 8080

# Use the ngrok URL in GitHub webhook
# Example: https://abc123.ngrok.io/github-webhook/
```

#### Option 2: Use localtunnel
```bash
# Install localtunnel
npm install -g localtunnel

# Expose Jenkins
lt --port 8080

# Use the URL in GitHub webhook
```

#### Option 3: Deploy Jenkins to Cloud
- Deploy Jenkins to AWS, Azure, or DigitalOcean
- Use public IP/domain for webhook

### SCM Polling Not Working

**Issue:** Backup polling doesn't trigger builds

**Solutions:**
1. Check cron syntax in Jenkinsfile: `H/5 * * * *` means every 5 minutes
2. Verify Git credentials are configured in Jenkins
3. Check Jenkins has access to GitHub repository

## Webhook Payload Example

When GitHub triggers the webhook, it sends this payload:

```json
{
  "ref": "refs/heads/dev",
  "repository": {
    "name": "ecommerce-microservice-backend-app",
    "full_name": "AIejoCastro/ecommerce-microservice-backend-app",
    "clone_url": "https://github.com/AIejoCastro/ecommerce-microservice-backend-app.git"
  },
  "pusher": {
    "name": "AIejoCastro"
  },
  "commits": [...]
}
```

Jenkins GitHub Plugin processes this and triggers the build for the `dev` branch.

## Current Configuration

The Jenkinsfile now includes:

```groovy
triggers {
    // GitHub webhook trigger
    githubPush()
    // Poll SCM every 5 minutes as backup
    pollSCM('H/5 * * * *')
}
```

## Verification

After setup, every push to the `dev` branch will:
1. GitHub sends webhook to Jenkins
2. Jenkins receives the webhook
3. Jenkins automatically starts a new build
4. Build runs through all stages:
   - Checkout
   - Build Services
   - Build Docker Images
   - Push to Registry
   - Deploy to Kubernetes
   - Smoke Tests

## Additional Resources

- [Jenkins GitHub Plugin Documentation](https://plugins.jenkins.io/github/)
- [GitHub Webhooks Guide](https://docs.github.com/en/developers/webhooks-and-events/webhooks)
- [ngrok Documentation](https://ngrok.com/docs)

## Support

If you encounter issues, check:
1. Jenkins Console Output for detailed error messages
2. GitHub webhook delivery status
3. Jenkins system logs
4. Network connectivity between GitHub and Jenkins
