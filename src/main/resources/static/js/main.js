document.addEventListener('DOMContentLoaded', () => {
    const contextPath = document.body.dataset.contextpath || '';

    // Carousel functionality
    const heroCarousel = document.querySelector('.hero-carousel');
    if (heroCarousel) {
        const heroTrack = heroCarousel.querySelector('.hero-track');
        const slides = Array.from(heroTrack.children);
        const nextButton = heroCarousel.querySelector('.hero-nav.next');
        const prevButton = heroCarousel.querySelector('.hero-nav.prev');
        const dotsContainer = heroCarousel.querySelector('.hero-dots');

        let slideWidth = slides[0].getBoundingClientRect().width;
        let currentIndex = 0;
        let autoPlayInterval;

        // Set up slides position
        const setSlidePosition = (slide, index) => {
            slide.style.left = slideWidth * index + 'px';
        };
        slides.forEach(setSlidePosition);

        // Create dots
        slides.forEach((_, index) => {
            const dot = document.createElement('button');
            dot.classList.add('dot');
            dot.setAttribute('aria-label', `${index + 1}번 슬라이드`);
            dot.setAttribute('role', 'tab');
            dot.setAttribute('aria-selected', index === 0 ? 'true' : 'false');
            dot.addEventListener('click', () => moveToSlide(index));
            dotsContainer.appendChild(dot);
        });

        const dots = Array.from(dotsContainer.children);

        const updateDots = () => {
            dots.forEach((dot, index) => {
                if (index === currentIndex) {
                    dot.classList.add('active');
                    dot.setAttribute('aria-selected', 'true');
                } else {
                    dot.classList.remove('active');
                    dot.setAttribute('aria-selected', 'false');
                }
            });
        };

        const moveToSlide = (targetIndex) => {
            heroTrack.style.transform = 'translateX(-' + slideWidth * targetIndex + 'px)';
            currentIndex = targetIndex;
            updateDots();
        };

        // Handle window resize
        window.addEventListener('resize', () => {
            slideWidth = slides[0].getBoundingClientRect().width;
            slides.forEach(setSlidePosition);
            moveToSlide(currentIndex);
        });

        // Navigation buttons
        prevButton.addEventListener('click', () => {
            let targetIndex = currentIndex - 1;
            if (targetIndex < 0) {
                targetIndex = slides.length - 1;
            }
            moveToSlide(targetIndex);
            resetAutoPlay();
        });

        nextButton.addEventListener('click', () => {
            let targetIndex = currentIndex + 1;
            if (targetIndex >= slides.length) {
                targetIndex = 0;
            }
            moveToSlide(targetIndex);
            resetAutoPlay();
        });

        // Autoplay
        const startAutoPlay = () => {
            const intervalTime = parseInt(heroCarousel.dataset.interval) || 3000;
            autoPlayInterval = setInterval(() => {
                let targetIndex = currentIndex + 1;
                if (targetIndex >= slides.length) {
                    targetIndex = 0;
                }
                moveToSlide(targetIndex);
            }, intervalTime);
        };

        const stopAutoPlay = () => {
            clearInterval(autoPlayInterval);
        };

        const resetAutoPlay = () => {
            stopAutoPlay();
            startAutoPlay();
        };

        if (heroCarousel.dataset.autoplay === 'true') {
            startAutoPlay();
            heroCarousel.addEventListener('mouseenter', stopAutoPlay);
            heroCarousel.addEventListener('mouseleave', startAutoPlay);
        }

        // Initial setup
        moveToSlide(currentIndex);
    }

    // Search form validation
    const searchForm = document.querySelector('.search-form form');
    const searchInput = searchForm ? searchForm.querySelector('input[name="query"]') : null;

    if (searchForm && searchInput) {
        searchForm.addEventListener('submit', (event) => {
            if (searchInput.value.trim() === '') {
                event.preventDefault(); // Prevent form submission
                alert('검색어를 입력해주세요.');
                searchInput.focus();
            }
        });
    }

    // Header navigation active state (simple example)
    const currentPath = window.location.pathname.replace(contextPath, '');
    const navLinks = document.querySelectorAll('header nav ul li a');
    navLinks.forEach(link => {
        const linkPath = link.getAttribute('href').replace(contextPath, '');
        if (currentPath === linkPath || (currentPath === '/' && linkPath === '/main')) { // Adjust for main page
            link.classList.add('active');
        }
    });

    // Steps timeline active dot
    const stepsTimelineDots = document.querySelectorAll('.steps-timeline .dot');
    if (stepsTimelineDots.length > 0) {
        // For now, only the first dot is active as per the JSP. 
        // More complex logic would be needed if this changes dynamically.
        stepsTimelineDots[0].classList.add('active');
    }
});
