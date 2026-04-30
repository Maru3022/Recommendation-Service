#!/bin/bash

# Docker Build Script for Recommendation Service
# Поддерживает локальные сборки и multi-platform сборки

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Defaults
REGISTRY="${REGISTRY:-ghcr.io}"
OWNER="${OWNER:-}"
IMAGE_NAME="recommendation-service"
BUILD_TYPE="local"  # local or multi-platform
PUSH="false"
LOAD="true"

# Functions
usage() {
    cat << EOF
${BLUE}Docker Build Script${NC}

Usage: $0 [OPTIONS]

Options:
    -h, --help              Show this help message
    -m, --multi-platform    Multi-platform build (amd64, arm64)
    -p, --push             Push to registry (requires -r and -o)
    -r, --registry REGISTRY Registry URL (default: ghcr.io)
    -o, --owner OWNER      Registry owner/org (required for push)
    -t, --tag TAG          Image tag (default: latest)
    
Examples:
    # Local build (native platform only)
    $0
    
    # Local build with specific tag
    $0 --tag v1.0.0
    
    # Multi-platform build (don't load)
    $0 --multi-platform --push -r ghcr.io -o myorg
    
    # Multi-platform build for testing
    $0 --multi-platform
EOF
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

check_buildx() {
    if ! docker buildx ls > /dev/null 2>&1; then
        log_error "Docker BuildX is not installed or configured"
        echo "Install it with: docker buildx create --name multiplatform"
        exit 1
    fi
    log_success "Docker BuildX is available"
}

check_registry_auth() {
    if [ "$PUSH" = "true" ]; then
        if [ -z "$OWNER" ]; then
            log_error "Owner is required for push operations (use -o flag)"
            exit 1
        fi
        log_success "Registry authentication required (will use existing Docker login)"
    fi
}

build_local() {
    log_info "Building for local platform..."
    
    local tag="${REGISTRY}/${OWNER:+${OWNER}/}${IMAGE_NAME}:${TAG}"
    
    if [ "$LOAD" = "true" ]; then
        log_info "Build args: --load -t $tag"
        docker buildx build \
            --load \
            --tag "$tag" \
            --sbom=true \
            --provenance=false \
            .
    else
        log_info "Build args: --push -t $tag"
        docker buildx build \
            --push \
            --tag "$tag" \
            --sbom=true \
            --provenance=true \
            .
    fi
}

build_multi_platform() {
    log_info "Building for multiple platforms (amd64, arm64)..."
    
    local platforms="linux/amd64,linux/arm64"
    local tag="${REGISTRY}/${OWNER:+${OWNER}/}${IMAGE_NAME}:${TAG}"
    
    if [ "$PUSH" = "true" ]; then
        if [ -z "$OWNER" ]; then
            log_error "Owner is required for push (use -o flag)"
            exit 1
        fi
        
        log_info "Build args: --platforms $platforms --push -t $tag"
        docker buildx build \
            --platforms "$platforms" \
            --push \
            --tag "$tag" \
            --sbom=true \
            --provenance=true \
            --cache-from=type=gha \
            --cache-to=type=gha,mode=max \
            .
        
        log_success "Image pushed to $tag"
    else
        log_warning "Multi-platform builds cannot use --load without pushing"
        log_warning "Attempting build without loading to local Docker..."
        
        docker buildx build \
            --platforms "$platforms" \
            --tag "$tag" \
            --sbom=true \
            --provenance=true \
            --output=type=docker,push=false \
            .
        
        log_success "Built successfully (not loaded to Docker daemon)"
    fi
}

# Parse arguments
TAG="latest"
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -m|--multi-platform)
            BUILD_TYPE="multi-platform"
            LOAD="false"
            shift
            ;;
        -p|--push)
            PUSH="true"
            shift
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -o|--owner)
            OWNER="$2"
            shift 2
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Main
main() {
    log_info "Recommendation Service Docker Build"
    echo ""
    
    check_buildx
    check_registry_auth
    
    echo ""
    log_info "Build Configuration:"
    echo "  Registry: $REGISTRY"
    echo "  Owner: ${OWNER:-(local)}"
    echo "  Image: $IMAGE_NAME"
    echo "  Tag: $TAG"
    echo "  Type: $BUILD_TYPE"
    echo "  Push: $PUSH"
    echo "  Load: $LOAD"
    echo ""
    
    if [ "$BUILD_TYPE" = "local" ]; then
        build_local
    else
        build_multi_platform
    fi
    
    echo ""
    log_success "Build completed!"
}

main "$@"
