#!/bin/bash
# Docker cleanup script to free up disk space

set -e

echo "🧹 Docker Cleanup Script"
echo "========================"
echo ""

# Show current disk usage
echo "📊 Current Docker disk usage:"
docker system df
echo ""

# Ask for confirmation
read -p "Do you want to proceed with cleanup? (y/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Cleanup cancelled"
    exit 0
fi

echo ""
echo "🧹 Starting cleanup..."
echo ""

# 1. Remove dangling images (untagged images)
echo "1️⃣ Removing dangling images..."
DANGLING=$(docker images -f "dangling=true" -q)
if [ -z "$DANGLING" ]; then
    echo "   ✓ No dangling images found"
else
    docker rmi $(docker images -f "dangling=true" -q) 2>/dev/null || echo "   ⚠️ Some dangling images couldn't be removed"
    echo "   ✓ Dangling images removed"
fi
echo ""

# 2. Remove unused images (not used by any container)
echo "2️⃣ Removing unused images..."
docker image prune -a -f
echo "   ✓ Unused images removed"
echo ""

# 3. Remove build cache
echo "3️⃣ Removing build cache..."
docker builder prune -a -f
echo "   ✓ Build cache removed"
echo ""

# 4. Remove stopped containers
echo "4️⃣ Removing stopped containers..."
docker container prune -f
echo "   ✓ Stopped containers removed"
echo ""

# 5. Remove unused networks
echo "5️⃣ Removing unused networks..."
docker network prune -f
echo "   ✓ Unused networks removed"
echo ""

# Show final disk usage
echo "📊 Final Docker disk usage:"
docker system df
echo ""

echo "✅ Cleanup completed!"

